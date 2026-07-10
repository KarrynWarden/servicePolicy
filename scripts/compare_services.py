#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Сверка старого (.asmx) и нового (/ws) SOAP-сервисов GetInsPrkState.

Читает CSV со входными записями (колонки в порядке BUFF_INSCHECK_IN), шлёт каждую
на оба сервиса, сравнивает ответы и генерирует ОДИН самодостаточный HTML-отчёт:
список nrec (зелёный — совпало, красный — различие, серый — ошибка), галочка
«скрыть совпавшие», по клику — полный ответ обеих функций и подсветка различий.

Только стандартная библиотека Python 3 — работает на офлайн-ПК без установки пакетов.

Колонки CSV (в этом порядке; первая строка-заголовок с 'nrec' распознаётся и пропускается):
  nrec,date1,date2,type_org,code_org,fam,im,ot,w,dr,vpolis,npolis,doctype,docser,docnum,ss,mr

Пример:
  python3 compare_services.py \
      --input inputs.csv \
      --old-url http://10.0.100.6/IASWeb/InsCheckTest/InsCheck.asmx \
      --new-url http://10.0.14.61:8080/ws \
      --out report.html
"""
import argparse
import csv
import html
import json
import re
import socket
import sys
import urllib.request
import urllib.error
import xml.etree.ElementTree as ET
from concurrent.futures import ThreadPoolExecutor
from xml.sax.saxutils import escape

# Имена ТЕГОВ запроса (как в контракте сервиса). Порядок = порядок колонок CSV.
# Внимание: 16-е поле — snils (в CSV/БД оно может называться "ss", но ТЕГ — snils).
FIELDS = ["nrec", "date1", "date2", "type_org", "code_org", "fam", "im", "ot",
          "w", "dr", "vpolis", "npolis", "doctype", "docser", "docnum", "snils", "mr"]

DATE_FIELDS = {"date1", "date2", "dr"}
_DDMMYYYY = re.compile(r"^(\d{2})\.(\d{2})\.(\d{4})$")
_YYYYMMDD = re.compile(r"^\d{4}-\d{2}-\d{2}$")


def norm_date(v):
    """dd.mm.yyyy -> yyyy-mm-dd; yyyy-mm-dd и прочее — без изменений."""
    v = (v or "").strip()
    m = _DDMMYYYY.match(v)
    if m:
        return "%s-%s-%s" % (m.group(3), m.group(2), m.group(1))
    return v


def build_envelope(row):
    parts = []
    for f in FIELDS:
        val = row.get(f, "")
        if f in DATE_FIELDS:
            val = norm_date(val)
        parts.append("        <%s>%s</%s>" % (f, escape(val or ""), f))
    body = "\n".join(parts)
    return (
        '<?xml version="1.0" encoding="utf-8"?>\n'
        '<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">\n'
        '  <soap:Body>\n'
        '    <GetInsPrkState xmlns="http://tempuri.org/">\n'
        '      <query>\n' + body + '\n'
        '      </query>\n'
        '    </GetInsPrkState>\n'
        '  </soap:Body>\n'
        '</soap:Envelope>\n'
    )


def post(url, body, timeout):
    """Возвращает (status, text). На HTTP-ошибке НЕ падает — тело читаем (там SOAP Fault)."""
    data = body.encode("utf-8")
    req = urllib.request.Request(url, data=data, method="POST")
    req.add_header("Content-Type", "text/xml; charset=utf-8")
    req.add_header("SOAPAction", '"http://tempuri.org/GetInsPrkState"')
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            return getattr(resp, "status", 200), resp.read().decode("utf-8", "replace")
    except urllib.error.HTTPError as e:
        # SOAP 1.1 отдаёт Fault со статусом 500 — тело важно, читаем его.
        try:
            body_txt = e.read().decode("utf-8", "replace")
        except Exception:
            body_txt = ""
        return e.code, body_txt


def local(tag):
    """Локальное имя тега без namespace."""
    return tag.split("}", 1)[1] if "}" in tag else tag


def find_first(elem, name):
    for e in elem.iter():
        if local(e.tag) == name:
            return e
    return None


def canon_lines(elem, depth=0, out=None):
    """Каноничный вид элемента: строки 'отступ tag' / 'отступ tag: значение'.
    Без namespace, в порядке документа — для сравнения и показа."""
    if out is None:
        out = []
    indent = "  " * depth
    name = local(elem.tag)
    kids = list(elem)
    if kids:
        out.append("%s%s" % (indent, name))
        for k in kids:
            canon_lines(k, depth + 1, out)
    else:
        text = (elem.text or "").strip()
        out.append("%s%s: %s" % (indent, name, text))
    return out


def parse_answer(resp_text):
    """Возвращает (lines, error). error — текст SOAP Fault или разбора, иначе None."""
    try:
        root = ET.fromstring(resp_text)
    except ET.ParseError as e:
        return None, "Не удалось разобрать XML: %s" % e
    fault = find_first(root, "Fault")
    if fault is not None:
        fs = find_first(fault, "faultstring")
        return None, "SOAP Fault: %s" % ((fs.text or "").strip() if fs is not None else "?")
    answer = find_first(root, "answer")
    if answer is None:
        return None, "В ответе нет <answer>"
    return canon_lines(answer), None


def call(url, row, timeout):
    try:
        status, text = post(url, build_envelope(row), timeout)
    except (urllib.error.URLError, socket.timeout, OSError) as e:
        return None, "Сеть: %s" % e
    lines, err = parse_answer(text)
    if lines is not None:
        return lines, None
    if err and err.startswith("SOAP Fault"):
        return None, err  # осмысленный фолт (например, из старого сервиса)
    snippet = " ".join((text or "").split())[:400]
    if status and status >= 400:
        return None, "HTTP %s: %s" % (status, snippet or err or "пустое тело")
    return None, err or ("пустой/непонятный ответ: " + snippet)


def read_rows(path):
    with open(path, newline="", encoding="utf-8-sig") as f:
        reader = csv.reader(f)
        rows = []
        for i, rec in enumerate(reader):
            if not rec or all((c or "").strip() == "" for c in rec):
                continue
            if i == 0 and rec[0].strip().lower() == "nrec":
                continue  # заголовок
            row = {}
            for j, f_ in enumerate(FIELDS):
                row[f_] = rec[j].strip() if j < len(rec) else ""
            rows.append(row)
        return rows


def process(rows, old_url, new_url, timeout, workers):
    results = [None] * len(rows)

    def work(idx):
        row = rows[idx]
        old_lines, old_err = call(old_url, row, timeout)
        new_lines, new_err = call(new_url, row, timeout)
        # mine = новый сервис (--new-url), orig = старый (--old-url).
        # Сравнение и дифф считаются в браузере (чтобы галочка «игнорировать p_*»
        # пересчитывала мгновенно) — здесь только сырые канонические строки.
        if old_err or new_err:
            item = {"nrec": row.get("nrec", ""), "status": "error",
                    "mine_err": new_err, "orig_err": old_err}
        else:
            item = {"nrec": row.get("nrec", ""), "status": "ok",
                    "mine": new_lines, "orig": old_lines}
        item["n"] = idx + 1   # уникальный номер строки ввода (nrec/полис может повторяться)
        results[idx] = item

    done = 0
    with ThreadPoolExecutor(max_workers=workers) as ex:
        for _ in ex.map(work, range(len(rows))):
            done += 1
            if done % 25 == 0 or done == len(rows):
                print("  обработано %d/%d" % (done, len(rows)), file=sys.stderr)
    return results


HTML_TMPL = r"""<!doctype html>
<html lang="ru"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Сверка GetInsPrkState</title>
<style>
  :root { --bg:#fff; --fg:#1a1a1a; --mut:#666; --line:#e3e3e3;
          --ok:#1a7f37; --okbg:#e7f5ec; --bad:#c9241c; --badbg:#fdeceb;
          --err:#8a6d00; --errbg:#fdf6e3; --chg:#fff3bf; --del:#fdeceb; --add:#e7f5ec; }
  @media (prefers-color-scheme: dark) {
    :root { --bg:#16181c; --fg:#e6e6e6; --mut:#9aa0a6; --line:#30343a;
            --ok:#4ac26b; --okbg:#12351f; --bad:#ff6b61; --badbg:#3a1614;
            --err:#e0c060; --errbg:#332b10; --chg:#4a3f10; --del:#3a1614; --add:#12351f; } }
  * { box-sizing: border-box; }
  body { margin:0; font:14px/1.45 system-ui,Segoe UI,Roboto,Arial,sans-serif;
         background:var(--bg); color:var(--fg); }
  header { position:sticky; top:0; background:var(--bg); border-bottom:1px solid var(--line);
           padding:12px 16px; z-index:5; }
  h1 { font-size:16px; margin:0 0 8px; }
  .sum { display:flex; gap:16px; flex-wrap:wrap; color:var(--mut); font-size:13px; align-items:center; }
  .sum b { color:var(--fg); }
  .pill { padding:1px 8px; border-radius:10px; font-weight:600; }
  .pill.ok { color:var(--ok); background:var(--okbg); }
  .pill.bad { color:var(--bad); background:var(--badbg); }
  .pill.err { color:var(--err); background:var(--errbg); }
  .controls { margin-top:8px; display:flex; gap:14px; align-items:center; flex-wrap:wrap; }
  input[type=search] { padding:5px 8px; border:1px solid var(--line); border-radius:6px;
                       background:var(--bg); color:var(--fg); min-width:180px; }
  main { padding:8px 16px 40px; }
  .row { border:1px solid var(--line); border-radius:8px; margin:6px 0; overflow:hidden; }
  .head { display:flex; align-items:center; gap:10px; padding:8px 12px; cursor:pointer; }
  .head:hover { background:rgba(127,127,127,.06); }
  .dot { width:10px; height:10px; border-radius:50%; flex:0 0 auto; }
  .dot.match{ background:var(--ok);} .dot.diff{ background:var(--bad);} .dot.error{ background:var(--err);}
  .num { font-family:ui-monospace,Menlo,Consolas,monospace; color:var(--mut); min-width:48px; }
  .nrec { font-family:ui-monospace,Menlo,Consolas,monospace; font-weight:600; }
  .tag { margin-left:auto; font-size:12px; color:var(--mut); }
  .detail { display:none; border-top:1px solid var(--line); padding:10px 12px; overflow-x:auto; }
  .row.open .detail { display:block; }
  table.diff { border-collapse:collapse; width:100%; font-family:ui-monospace,Menlo,Consolas,monospace;
               font-size:12.5px; }
  table.diff td { padding:1px 8px; vertical-align:top; white-space:pre; border-top:1px solid transparent; }
  table.diff td.lbl { color:var(--mut); text-align:right; width:70px; user-select:none; }
  tr.chg td.l, tr.chg td.r { background:var(--chg); }
  tr.del td.l { background:var(--del); }
  tr.add td.r { background:var(--add); }
  .colh td { color:var(--mut); font-weight:600; border-bottom:1px solid var(--line); white-space:normal; }
  .errbox { color:var(--bad); }
  .hint { color:var(--mut); font-size:12px; }
</style></head>
<body>
<header>
  <h1>Сверка GetInsPrkState — старый vs новый сервис</h1>
  <div class="sum">
    <span>Всего: <b id="s-total">0</b></span>
    <span class="pill ok">совпало: <span id="s-match">0</span></span>
    <span class="pill bad">различий: <span id="s-diff">0</span></span>
    <span class="pill err">ошибок: <span id="s-err">0</span></span>
  </div>
  <div class="controls">
    <label><input type="checkbox" id="hideMatch" checked> скрыть совпавшие</label>
    <label><input type="checkbox" id="ignoreP"> игнорировать p_disp/p_proph/p_healthc</label>
    <label><input type="checkbox" id="ignore206"> игнорировать ошибку 206</label>
    <label><input type="checkbox" id="ignore111txt"> игнорировать текст ошибки 111</label>
    <label><input type="checkbox" id="ignoreHalg"> не считать различием alg при H-алгоритме</label>
    <input type="search" id="q" placeholder="поиск по # или nrec…">
    <span class="hint">клик по строке — полный ответ и различия</span>
  </div>
</header>
<main id="list"></main>
<script>
const DATA = __DATA__;
const list = document.getElementById('list');
const IGN = ['p_disp','p_proph','p_healthc'];

function esc(s){ return (s==null?'':String(s)).replace(/[&<>]/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;'}[c])); }
function ignLineP(l){ const m=l.match(/^\s*([^:\s]+)\s*:/); return m && IGN.includes(m[1]); }
// В ответе есть H-алгоритм (H01/H02/H03 — латинская H или кириллическая Н)?
function hasHalg(lines){
  for(const l of lines){
    const m=l.match(/^\s*alg\s*:\s*(.+)$/);
    if(m){ const v=m[1].trim(); if(v[0]==='H'||v[0]==='Н') return true; }
  }
  return false;
}
// Фильтрует строки по активным галочкам: p_disp/p_proph/p_healthc, ошибка 206,
// и (dropAlg) строки alg — когда у ответа H-алгоритм.
// Для 206 убирается блок <err>(err/errcode:206/errtext) и строка ack (ack зависит от 206).
function filt(lines, dropAlg){
  const igP = document.getElementById('ignoreP').checked;
  const ig206 = document.getElementById('ignore206').checked;
  const ig111 = document.getElementById('ignore111txt').checked;
  if(!igP && !ig206 && !ig111 && !dropAlg) return lines;
  const out=[];
  for(let i=0;i<lines.length;i++){
    const l=lines[i];
    if(igP && ignLineP(l)) continue;
    if(dropAlg && /^\s*alg\s*:/.test(l)) continue;
    if(ig206 && /^\s*ack\s*:/.test(l)) continue;
    // Блок ошибки: err / errcode: X / errtext: Y
    if(/^\s*err\s*$/.test(l) && i+1<lines.length && /^\s*errcode\s*:/.test(lines[i+1])){
      const code = (lines[i+1].match(/errcode\s*:\s*(\d+)/)||[])[1];
      if(ig206 && code==='206'){ i+=2; continue; }              // 206: убрать весь блок (err+errcode+errtext)
      if(ig111 && code==='111'){                                 // 111: убрать только errtext (текст изменён)
        out.push(l); out.push(lines[i+1]); i+=2; continue;
      }
    }
    out.push(l);
  }
  return out;
}
function dropAlgFor(d){
  return document.getElementById('ignoreHalg').checked && (hasHalg(d.mine) || hasHalg(d.orig));
}
function eqArr(a,b){ return a.length===b.length && a.every((x,k)=>x===b[k]); }

// Дифф двух списков строк через LCS -> выровненные [op,l,r], op in eq/del/add.
function lcsDiff(a,b){
  const n=a.length, m=b.length;
  const dp=Array.from({length:n+1},()=>new Int32Array(m+1));
  for(let i=n-1;i>=0;i--) for(let j=m-1;j>=0;j--)
    dp[i][j]= a[i]===b[j] ? dp[i+1][j+1]+1 : Math.max(dp[i+1][j],dp[i][j+1]);
  const rows=[]; let i=0,j=0;
  while(i<n&&j<m){
    if(a[i]===b[j]){ rows.push(['eq',a[i],b[j]]); i++; j++; }
    else if(dp[i+1][j]>=dp[i][j+1]){ rows.push(['del',a[i],null]); i++; }
    else { rows.push(['add',null,b[j]]); j++; }
  }
  while(i<n){ rows.push(['del',a[i++],null]); }
  while(j<m){ rows.push(['add',null,b[j++]]); }
  return rows;
}

function statusOf(d){
  if(d.status==='error') return 'error';
  const da=dropAlgFor(d);
  return eqArr(filt(d.mine,da), filt(d.orig,da)) ? 'match' : 'diff';
}

function detailHTML(d){
  if(d.status==='error'){
    return '<div class="errbox">Ошибка запроса.<br>Моя функция (новый): '+esc(d.mine_err||'—')+
           '<br>Оригинал (старый): '+esc(d.orig_err||'—')+'</div>';
  }
  const da=dropAlgFor(d);
  const mine=filt(d.mine,da), orig=filt(d.orig,da);
  const rows = eqArr(mine,orig) ? mine.map(l=>['eq',l,l]) : lcsDiff(mine,orig);
  let h = '<table class="diff"><tr class="colh"><td>Моя функция</td><td>Оригинал</td></tr>';
  for(const [op,l,r] of rows){
    const cls = op==='eq' ? '' : op;
    h += '<tr class="'+cls+'"><td class="l">'+esc(l)+'</td><td class="r">'+esc(r)+'</td></tr>';
  }
  return h + '</table>';
}

function render(){
  const hide = document.getElementById('hideMatch').checked;
  const q = document.getElementById('q').value.trim().toLowerCase();
  let sM=0,sD=0,sE=0;
  list.innerHTML='';
  for(const d of DATA){
    const st = statusOf(d);
    if(st==='match')sM++; else if(st==='diff')sD++; else sE++;
    if(hide && st==='match') continue;
    if(q && !(String(d.n).includes(q) || String(d.nrec).toLowerCase().includes(q))) continue;
    const row = document.createElement('div');
    row.className='row';
    const label = st==='match'?'совпало':(st==='diff'?'различие':'ошибка');
    row.innerHTML = '<div class="head"><span class="dot '+st+'"></span>'+
      '<span class="num">#'+d.n+'</span>'+
      '<span class="nrec">'+esc(d.nrec)+'</span><span class="tag">'+label+'</span></div>'+
      '<div class="detail"></div>';
    const head = row.querySelector('.head');
    const det = row.querySelector('.detail');
    head.addEventListener('click', ()=>{
      if(!row.classList.contains('open') && !det.dataset.built){
        det.innerHTML = detailHTML(d); det.dataset.built='1';
      }
      row.classList.toggle('open');
    });
    list.appendChild(row);
  }
  document.getElementById('s-total').textContent = DATA.length;
  document.getElementById('s-match').textContent = sM;
  document.getElementById('s-diff').textContent = sD;
  document.getElementById('s-err').textContent = sE;
  if(!list.children.length){
    list.innerHTML='<p class="hint">Нет строк для показа (снимите галочку или измените поиск).</p>';
  }
}
function onFilterChange(){
  // при смене фильтра пересобрать уже раскрытые детали и пересчитать статусы
  document.querySelectorAll('.detail').forEach(el=>{ el.dataset.built=''; el.innerHTML=''; });
  render();
}
document.getElementById('hideMatch').addEventListener('change', render);
document.getElementById('ignoreP').addEventListener('change', onFilterChange);
document.getElementById('ignore206').addEventListener('change', onFilterChange);
document.getElementById('ignore111txt').addEventListener('change', onFilterChange);
document.getElementById('ignoreHalg').addEventListener('change', onFilterChange);
document.getElementById('q').addEventListener('input', render);
render();
</script>
</body></html>
"""


def generate_html(results, out_path):
    data_json = json.dumps(results, ensure_ascii=False)
    doc = HTML_TMPL.replace("__DATA__", data_json)
    with open(out_path, "w", encoding="utf-8") as f:
        f.write(doc)


def main():
    ap = argparse.ArgumentParser(description="Сверка старого и нового SOAP-сервисов GetInsPrkState")
    ap.add_argument("--input", required=True, help="CSV со входными записями")
    ap.add_argument("--old-url", required=True, help="URL старого сервиса (.asmx)")
    ap.add_argument("--new-url", required=True, help="URL нового сервиса (/ws)")
    ap.add_argument("--out", default="report.html", help="HTML-отчёт (по умолчанию report.html)")
    ap.add_argument("--timeout", type=float, default=30, help="таймаут запроса, сек")
    ap.add_argument("--workers", type=int, default=8, help="параллельных запросов")
    args = ap.parse_args()

    rows = read_rows(args.input)
    print("Записей на вход: %d" % len(rows), file=sys.stderr)
    if not rows:
        print("Пустой ввод — нечего сверять.", file=sys.stderr)
        return 1
    results = process(rows, args.old_url, args.new_url, args.timeout, args.workers)
    generate_html(results, args.out)
    e = sum(1 for r in results if r["status"] == "error")
    m = sum(1 for r in results if r["status"] == "ok" and r["mine"] == r["orig"])
    d = sum(1 for r in results if r["status"] == "ok" and r["mine"] != r["orig"])
    print("Готово: %s  (совпало %d, различий %d, ошибок %d)" % (args.out, m, d, e), file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main())
