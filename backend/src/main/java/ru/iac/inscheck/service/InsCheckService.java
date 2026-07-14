package ru.iac.inscheck.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.iac.inscheck.dao.InsCheckDao;
import ru.iac.inscheck.dao.InsCheckLimitDao;
import ru.iac.inscheck.dao.InsCheckLogDao;
import ru.iac.inscheck.model.Candidate;
import ru.iac.inscheck.model.ErrCode;
import ru.iac.inscheck.model.IPerson;
import ru.iac.inscheck.model.IPrkDept;
import ru.iac.inscheck.model.SearchAlgorithm;
import ru.iac.inscheck.model.SearchParams;
import ru.iac.inscheck.ws.model.Answer;
import ru.iac.inscheck.ws.model.Err;
import ru.iac.inscheck.ws.model.Ins;
import ru.iac.inscheck.ws.model.Prk;
import ru.iac.inscheck.ws.model.Query;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Бизнес-логика операции GetInsPrkState (разделы 2–6 постановки).
 *
 * Соответствует методу Service.GetInsPrkState + Oracle-пакету inscheck.GetAnswer
 * из старой системы (сам пакет в исходниках отсутствовал, логика восстановлена
 * по постановке). Добавлен новый этап идентификации H (H01/H02/H03).
 */
@Service
public class InsCheckService {

    private static final Logger log = LoggerFactory.getLogger(InsCheckService.class);

    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String IP_TFOMS = "10.0.14.46";
    private static final String METHOD = "GetInsPrkState";

    // Разделы алгоритмов поиска (порядок Таблицы 1): С → Р → H (новый) → В.
    // Внутри раздела С и В — до первого найденного факта страхования (п.4.2.2).
    // Р01 — не самостоятельный шаг, а дополнение найденной СП (см. runSearch).
    private static final List<SearchAlgorithm> SECTION_C =
            List.of(SearchAlgorithm.C01, SearchAlgorithm.C02, SearchAlgorithm.C03);
    private static final List<SearchAlgorithm> SECTION_H =
            List.of(SearchAlgorithm.H01, SearchAlgorithm.H02, SearchAlgorithm.H03);
    private static final List<SearchAlgorithm> SECTION_V =
            List.of(SearchAlgorithm.V01, SearchAlgorithm.V02, SearchAlgorithm.V03,
                    SearchAlgorithm.V04, SearchAlgorithm.V05, SearchAlgorithm.V06);

    private final InputValidator validator;
    private final InsCheckDao dao;
    private final InsCheckLogDao logDao;
    private final InsCheckLimitDao limitDao;

    public InsCheckService(InputValidator validator, InsCheckDao dao,
                           InsCheckLogDao logDao, InsCheckLimitDao limitDao) {
        this.validator = validator;
        this.dao = dao;
        this.logDao = logDao;
        this.limitDao = limitDao;
    }

    /**
     * Основной DB-поток (аналог транзакции в старом сервисе после GIPSV2_checkInParam).
     * Структурная проверка входа выполняется РАНЬШЕ, в контроллере (до открытия
     * транзакции) — поэтому здесь вход уже структурно корректен.
     */
    @Transactional
    public Answer getInsPrkState(Query q, RequestContext ctx) {
        Answer answer = new Answer();
        answer.setNrec(q.getNrec());

        Set<ErrCode> errors = new LinkedHashSet<>();
        try {
            // 2. Проверка данных (контроли 100–111, аналог inscheck.checkIn)
            Validation validation = validator.validate(q);
            errors.addAll(validation.getErrors());
            SearchParams sp = validation.getParams();

            // Контроль 5 (дневной лимит запросов) в старом сервисе НЕ реализован
            // (таблица InscheckLimit не используется) — отключён для совпадения поведения.
            // checkAndCountLimit(ctx.getRemoteIp(), errors);

            // 108. Номер полиса отсутствует в Регистре (проверка по данным)
            if (sp.hasPolis() && !dao.npolisExists(sp.getVpolis(), sp.getNpolis())) {
                errors.add(ErrCode.E108);
            }

            // 4–6. Поиск и формирование ответа выполняются только без фатальных ошибок
            boolean fatal = errors.stream().anyMatch(ErrCode::isFatal);
            if (!fatal) {
                processSearch(q, sp, answer, errors);
            }
        } catch (RuntimeException e) {
            // Аналог общего catch в старом коде: код 2 — непредвиденная ошибка.
            // Логируем причину (обычно SQL/схема) — иначе errcode 2 ничего не объясняет.
            log.error("Ошибка обработки GetInsPrkState (nrec={}): {}", q.getNrec(), e.toString(), e);
            errors.add(ErrCode.STRUCT);
        }

        // 3.1. Журнал операций (как в старом сервисе — при нормальном потоке)
        writeLog(q, ctx, errors);

        fillErrors(answer, errors);
        answer.setAck(errors.isEmpty() && answer.getIns() != null ? "0" : "2");
        return answer;
    }

    // ===== 3.2. Лимит запросов =====

    private void checkAndCountLimit(String ip, Set<ErrCode> errors) {
        if (ip == null) {
            return;
        }
        InsCheckLimitDao.LimitRow row = limitDao.select(ip);
        if (row == null) {
            limitDao.insert(ip);
        } else {
            if (row.exceededToday()) {
                errors.add(ErrCode.LIMIT);
            }
            limitDao.update(ip);
        }
    }

    // ===== 4–6. Поиск СП, прикрепления, формирование ответа =====

    private void processSearch(Query q, SearchParams sp, Answer answer, Set<ErrCode> errors) {
        LocalDate date1 = LocalDate.parse(q.getDate1(), ISO);
        LocalDate date2 = LocalDate.parse(q.getDate2(), ISO);

        // 4.2–4.3. Поиск СП по разделам С → Р → H → В.
        SearchResult sr = runSearch(sp);
        if (sr.found.isEmpty()) {
            errors.add(ErrCode.E200); // СП не найдена
            return;
        }
        // Код(ы) алгоритма в ответе. Старый сервис отдаёт alg НЕСКОЛЬКИМИ тегами <alg>,
        // разбивая строку ls_alg по запятой (напр. "С01, Р01" → "С01" и " Р01").
        String algJoined = String.join(", ", sr.algCodes);
        for (String part : algJoined.split(",")) {
            answer.getAlg().add(part);
        }

        // 4.4. СК* = записи с одинаковым IDMain. Контроль 201 — найдено более одного ЗЛ.
        Set<Long> skStar = sr.found.values().stream()
                .map(c -> c.idmain() != null ? c.idmain() : c.id())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (skStar.size() > 1) {
            errors.add(ErrCode.E201);
            return;
        }

        Candidate hit = sr.found.values().iterator().next();
        List<IPerson> members = dao.findSkMembers(hit.idmain(), hit.id());
        IPerson pzsk = selectPzsk(members, date1, date2);

        // Диагностика поиска СП/ПЗСК (включается logging.level.ru.iac.inscheck=DEBUG).
        if (log.isDebugEnabled()) {
            log.debug("nrec={} alg={} кандидаты={} членов_СК={}",
                    q.getNrec(), sr.algCodes,
                    sr.found.values().stream().map(c -> c.id() + "/" + c.idmain()).toList(),
                    members.size());
            for (IPerson m : members) {
                log.debug("  член СК: id={} idmain={} dbeg={} dvizit={} dend={} reason={} vpolis={} smo={}",
                        m.getId(), m.getIdmain(), m.getDbeg(), m.getDvizit(), m.getDend(),
                        m.getReason(), m.getVpolis(), m.getSmo());
            }
            log.debug("  выбрана ПЗСК: id={}", pzsk == null ? null : pzsk.getId());
        }

        if (pzsk == null) {
            errors.add(ErrCode.E200);
            return;
        }

        // 6.1. Сведения о СП (ins) выдаются, даже если есть нефатальные ошибки (п.3.5.1.3)
        answer.setIns(buildIns(pzsk));

        // Раздел 3 Таблицы 4: соответствие данных Регистру (202–208)
        checkRegisterMatch(sp, pzsk, errors);

        // Признаки диспансеризации заполняются при определении СП (строка RES есть),
        // независимо от актуальности — как в старом сервисе. Год берётся по Date1
        // (постановка, столбец «Заполнение»: YEAR принадлежит Date1).
        fillHealthFlags(answer, pzsk.getId(), date1.getYear());

        // 5. Прикрепление. Как в старом сервисе: строка <prk> выдаётся ВСЕГДА при найденной
        // СП (пустая, если прикрепления на период нет) — один prk (max dbeg, max typeprk).
        IPrkDept prk = dao.findPrk(pzsk.getId(), date1, date2);
        answer.getPrk().add(buildPrk(prk));

        // Раздел 4 Таблицы 4: особенности найденной СП (300–305)
        boolean actual = isActual(pzsk, date1, date2);
        if (!actual) {
            errors.add(notActualReason(pzsk, date2));
        } else if (prk == null) {
            // Контроль 305 — только при актуальной СП и отсутствии прикрепления.
            errors.add(ErrCode.E305);
            if (log.isDebugEnabled()) {   // диагностика: все строки iprkdept по id (без фильтра дат)
                List<IPrkDept> all = dao.findAllPrk(pzsk.getId());
                log.debug("305: прикрепления по id={} (всего {}):", pzsk.getId(), all.size());
                for (IPrkDept d : all) {
                    log.debug("  iprkdept: id={} dbeg={} dend={} typeprk={} mo={}",
                            d.getId(), d.getDbeg(), d.getDend(), d.getTypeprk(), d.getMo());
                }
            }
        }
    }

    /**
     * Поиск СП по разделам Таблицы 1 (перенос checkAlg):
     *   С01→С02→С03 — до первого найденного;
     *   Р01 — дополняет найденную С-запись (дописывает ", Р01") либо, если С ничего не
     *         дал, сам добавляет найденную СП (аналог MERGE из пакета) и блокирует H/В;
     *   H01→H03, затем В01→В06 — только если СП ещё не найдена, каждый до первого.
     */
    private SearchResult runSearch(SearchParams sp) {
        SearchResult r = new SearchResult();

        // Раздел С: первый непустой из С01/С02/С03
        firstHit(SECTION_C, sp, r);

        // Раздел Р (Р01): дополнение уже найденной СП либо добавление своей
        if (applicable(SearchAlgorithm.R01, sp)) {
            List<Candidate> hits = dao.search(SearchAlgorithm.R01, sp);
            if (!hits.isEmpty()) {
                // matched (id уже найден в С) → дописываем Р01; not matched → добавляем запись
                hits.forEach(c -> r.found.putIfAbsent(c.id(), c));
                r.algCodes.add(SearchAlgorithm.R01.getCode());
            }
        }

        // Разделы H (новый) и В — только если СП ещё не найдена
        if (r.found.isEmpty()) {
            firstHit(SECTION_H, sp, r);
        }
        if (r.found.isEmpty()) {
            firstHit(SECTION_V, sp, r);
        }
        return r;
    }

    /** Выполняет алгоритмы раздела по порядку до первого непустого результата. */
    private void firstHit(List<SearchAlgorithm> section, SearchParams sp, SearchResult r) {
        for (SearchAlgorithm alg : section) {
            if (!applicable(alg, sp)) {
                continue;
            }
            List<Candidate> hits = dao.search(alg, sp);
            if (!hits.isEmpty()) {
                hits.forEach(c -> r.found.putIfAbsent(c.id(), c));
                r.algCodes.add(alg.getCode());
                return;
            }
        }
    }

    /** Результат поиска: найденные кандидаты по id и коды сработавших алгоритмов. */
    private static final class SearchResult {
        final LinkedHashMap<Long, Candidate> found = new LinkedHashMap<>();
        final List<String> algCodes = new ArrayList<>();
    }

    /** Применимость алгоритма: если нужный реквизит отсутствует — поиск не ведётся (п.4.2.1). */
    private boolean applicable(SearchAlgorithm alg, SearchParams sp) {
        return switch (alg) {
            case C01 -> sp.hasFio() && (sp.hasDr() || sp.hasMr()) && sp.hasPolis();
            case C02 -> sp.hasFio() && (sp.hasDr() || sp.hasMr()) && sp.hasDoc();
            case C03 -> sp.hasFio() && (sp.hasDr() || sp.hasMr()) && sp.hasSnils();
            case R01 -> sp.hasFio() && sp.hasDr() && sp.hasPolis();
            case H01 -> sp.hasFio() && sp.hasDr() && sp.hasPolis();
            case H02 -> sp.hasFio() && sp.hasDr() && sp.hasDoc();
            case H03 -> sp.hasFio() && sp.hasDr() && sp.hasSnils();
            case V01 -> sp.hasFio() && (sp.hasDr() || sp.hasMr()) && sp.hasPolis();
            case V02 -> sp.hasFio() && (sp.hasDr() || sp.hasMr()) && sp.hasDoc();
            case V03 -> sp.hasFio() && (sp.hasDr() || sp.hasMr()) && sp.hasSnils();
            case V04 -> sp.hasPolis();
            case V05 -> sp.hasFio() && sp.hasDr();
            case V06 -> sp.hasFio() && sp.hasDr();
        };
    }

    /** 4.5. Выбор ПЗСК* — хронологически ближайшей к периоду поиска записи комплекта. */
    IPerson selectPzsk(List<IPerson> members, LocalDate date1, LocalDate date2) {
        if (members == null || members.isEmpty()) {
            return null;
        }
        // a) Max(Dbeg) среди Reason≠5 и [date1;date2] in [dvizit;dend]
        IPerson r = members.stream()
                .filter(p -> notReason5(p) && periodIn(p, date1, date2))
                .max(byDbeg()).orElse(null);
        // b) Max(Dbeg) среди [date1;date2] in [dvizit;dend]
        if (r == null) {
            r = members.stream().filter(p -> periodIn(p, date1, date2)).max(byDbeg()).orElse(null);
        }
        // c) Min(Dbeg) среди Reason≠5 и DVizit > Date2
        if (r == null) {
            r = members.stream().filter(p -> notReason5(p) && after(p.getDvizit(), date2))
                    .min(byDbeg()).orElse(null);
        }
        // d) Min(Dbeg) среди DVizit > Date2
        if (r == null) {
            r = members.stream().filter(p -> after(p.getDvizit(), date2)).min(byDbeg()).orElse(null);
        }
        // e) Max(Dbeg) среди Reason≠5 (по пакету — без условия DVizit < Date1)
        if (r == null) {
            r = members.stream().filter(InsCheckService::notReason5).max(byDbeg()).orElse(null);
        }
        // f) Max(Dbeg)
        if (r == null) {
            r = members.stream().max(byDbeg()).orElse(null);
        }
        return r;
    }

    // ===== Проверки соответствия Регистру (202–208) =====

    private void checkRegisterMatch(SearchParams sp, IPerson p, Set<ErrCode> errors) {
        if (sp.hasFio() && !(eq(sp.getFam(), p.getFam()) && eq(sp.getIm(), p.getIm()) && eq(sp.getOt(), p.getOt()))) {
            errors.add(ErrCode.E202);
        }
        if (sp.getW() != null && !sp.getW().equals(p.getW())) {
            errors.add(ErrCode.E203);
        }
        if (sp.getDr() != null && !LocalDate.parse(sp.getDr(), ISO).equals(p.getDr())) {
            errors.add(ErrCode.E204);
        }
        if (sp.hasPolis() && !(eqInt(sp.getVpolis(), p.getVpolis()) && eq(sp.getNpolis(), p.getNpolis()))) {
            errors.add(ErrCode.E205);
        }
        // 206: документ ЗЛ — в IDOC (связь по IDROW, TYPEROW in (1,2)), не в iperson.
        if (sp.hasDoc() && !dao.docMatches(p.getIdrow(), sp.getDoctype(), sp.getDocser(), sp.getDocnum())) {
            errors.add(ErrCode.E206);
        }
        if (sp.hasSnils() && !eq(sp.getSnils(), p.getSs())) {
            errors.add(ErrCode.E207);
        }
        if (sp.hasMr()) {
            // Перенос пакета: 208, если jaro_winkler(upper(СП.mr), upper(вход.mr)) < 0.8.
            double sim = p.getMr() == null ? 0.0 : dao.mrSimilarity(sp.getMr(), p.getMr());
            if (sim < 0.8) {
                errors.add(ErrCode.E208);
            }
        }
    }

    /** 300–304: причина «неактуальности» СП на период поиска. */
    private ErrCode notActualReason(IPerson p, LocalDate date2) {
        Integer reason = p.getReason();
        if ((reason != null && reason == 1) || p.getDdeath() != null) {
            return ErrCode.E300;
        }
        if (reason != null && (reason == 3 || reason == 93 || reason == 96)) {
            return ErrCode.E301;
        }
        if (reason != null && reason == 9) {
            return ErrCode.E302;
        }
        if (p.getDvizit() != null && !p.getDvizit().isBefore(date2)) { // DVizit >= Date2
            return ErrCode.E303;
        }
        return ErrCode.E304; // полис закрыт в РС ЕРЗ
    }

    // ===== Признаки диспансеризации (p_disp/p_proph/p_healthc) =====

    private void fillHealthFlags(Answer answer, long personId, int year) {
        // Старый сервис отдаёт p_disp/p_proph/p_healthc как "0"/"1" (сырое значение
        // BUFF_INSCHECK_RES), НЕ "Да"/"Нет" (постановка расходится с реализацией —
        // ориентир на старый сервис). MEDREE_PRDISP.groupcode: 1 — дисп., 2 — проф., 3 — ЦЗ.
        answer.setP_disp(flag01(dao.hasDisp(personId, 1, year)));
        answer.setP_proph(flag01(dao.hasDisp(personId, 2, year)));
        answer.setP_healthc(flag01(dao.hasDisp(personId, 3, year)));
    }

    // ===== Сборка элементов ответа =====

    private Ins buildIns(IPerson p) {
        Ins ins = new Ins();
        ins.setSmo(str(p.getSmo()));
        ins.setVpolis(str(p.getVpolis()));
        // Перенос пакета: fpolis выдаётся только для vpolis=3, иначе пусто;
        // npolis = ЕНП при vpolis=3, иначе номер полиса
        // (decode(vpolis,3,fpolis,null), decode(vpolis,3,enp,npolis)).
        boolean enp = p.getVpolis() != null && p.getVpolis() == 3;
        ins.setFpolis(enp ? str(p.getFpolis()) : "");
        // Восстановление ведущих нулей: в PG номер хранится числом и нули при чтении
        // теряются, старый сервис всегда добивал номер слева нулями до 16 знаков
        // (для всех типов полиса, включая vpolis=1). Пустой номер оставляем пустым.
        String num = nz(enp ? p.getEnp() : p.getNpolis());
        ins.setNpolis(num.isEmpty() ? "" : lpad0(num, 16));
        ins.setDvisit(date(p.getDvizit()));
        ins.setDbeg(date(p.getDbeg()));
        ins.setDend(date(p.getDend()));
        ins.setReason(str(p.getReason()));
        ins.setId(String.valueOf(p.getId()));
        return ins;
    }

    private Prk buildPrk(IPrkDept p) {
        Prk prk = new Prk();
        if (p == null) {
            // Прикрепления нет — пустая строка <prk><mo/><podr/><modt/> (как старый сервис).
            prk.setMo("");
            prk.setPodr("");
            prk.setModt("");
        } else {
            prk.setMo(str(p.getMo()));
            prk.setPodr(p.podr());
            prk.setModt(date(p.getDbeg()));
        }
        return prk;
    }

    // ===== 3.1. Журнал =====

    private void writeLog(Query q, RequestContext ctx, Set<ErrCode> errors) {
        try {
            String inpar = buildInpar(q);
            String errList = errors.stream().map(e -> String.valueOf(e.getCode()))
                    .collect(Collectors.joining(","));
            String iptfoms = IP_TFOMS.equals(ctx.getLocalIp()) ? IP_TFOMS : null;
            String login = ctx.getLogin() == null ? "" : ctx.getLogin();
            logDao.insertLog(METHOD, login, ctx.getRemoteIp(), inpar, errList,
                    q == null ? null : q.getType_org(), q == null ? null : q.getCode_org(), iptfoms);
        } catch (RuntimeException ignore) {
            // Журналирование не должно ломать ответ клиенту
        }
    }

    /** INPAR: «тег=значение» по непустым тегам запроса через «;» (раздел 3.1). */
    private String buildInpar(Query q) {
        if (q == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        append(sb, "nrec", q.getNrec());
        append(sb, "date1", q.getDate1());
        append(sb, "date2", q.getDate2());
        append(sb, "type_org", q.getType_org());
        append(sb, "code_org", q.getCode_org());
        append(sb, "fam", q.getFam());
        append(sb, "im", q.getIm());
        append(sb, "ot", q.getOt());
        append(sb, "w", q.getW());
        append(sb, "dr", q.getDr());
        append(sb, "vpolis", q.getVpolis());
        append(sb, "npolis", q.getNpolis());
        append(sb, "doctype", q.getDoctype());
        append(sb, "docser", q.getDocser());
        append(sb, "docnum", q.getDocnum());
        append(sb, "snils", q.getSnils());
        append(sb, "mr", q.getMr());
        return sb.toString();
    }

    private void fillErrors(Answer answer, Set<ErrCode> errors) {
        // Сортировка по коду — как в старом ответе (xmlagg ... order by err)
        errors.stream()
                .sorted(Comparator.comparingInt(ErrCode::getCode))
                .forEach(e -> answer.getErr().add(new Err(String.valueOf(e.getCode()), e.getText())));
    }

    // ===== helpers =====

    private static boolean notReason5(IPerson p) {
        return p.getReason() == null || p.getReason() != 5;
    }

    private static boolean periodIn(IPerson p, LocalDate date1, LocalDate date2) {
        // Перенос пакета: запись актуальна на период, если date1 <= dend И dvizit <= date2
        // (пересечение периодов, оригинал: b.date1<=a.dend and a.dvizit<=b.date2).
        return p.getDvizit() != null && p.getDend() != null
                && !date1.isAfter(p.getDend())          // date1 <= dend
                && !p.getDvizit().isAfter(date2);       // dvizit <= date2
    }

    private boolean isActual(IPerson p, LocalDate date1, LocalDate date2) {
        // Актуальность для проверок 300–305 — только пересечение периодов (как в пакете);
        // reason учитывается отдельно при выборе ПЗСК и в notActualReason.
        return periodIn(p, date1, date2);
    }

    private static boolean after(LocalDate d, LocalDate ref) {
        return d != null && d.isAfter(ref);
    }

    private static Comparator<IPerson> byDbeg() {
        return Comparator.comparing(IPerson::getDbeg, Comparator.nullsFirst(Comparator.naturalOrder()));
    }

    private static boolean eq(String a, String b) {
        return Objects.equals(a == null ? null : a.trim().toUpperCase(),
                b == null ? null : b.trim().toUpperCase());
    }

    private static boolean eqInt(Integer a, Integer b) {
        return Objects.equals(a, b);
    }

    // Старый сервис (VB dr("x").ToString) выводит теги ins/prk всегда — пустыми при null.
    // Поэтому в сборке ответа null превращаем в "" (JAXB отдаст <tag/>).
    private static String str(Integer v) {
        return v == null ? "" : String.valueOf(v);
    }

    private static String date(LocalDate d) {
        return d == null ? "" : d.format(ISO);
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    /** Дополнение слева нулями до значности len (восстановление ведущих нулей номера полиса). */
    private static String lpad0(String s, int len) {
        if (s == null) return "";
        return s.length() >= len ? s : "0".repeat(len - s.length()) + s;
    }

    private static String flag01(boolean b) {
        return b ? "1" : "0";
    }

    private static void append(StringBuilder sb, String tag, String value) {
        if (value != null && !value.trim().isEmpty()) {
            sb.append(tag).append('=').append(value.trim()).append("; ");
        }
    }
}
