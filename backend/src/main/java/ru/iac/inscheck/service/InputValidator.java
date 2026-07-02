package ru.iac.inscheck.service;

import org.springframework.stereotype.Component;
import ru.iac.inscheck.dao.InsCheckDao;
import ru.iac.inscheck.model.ErrCode;
import ru.iac.inscheck.model.SearchParams;
import ru.iac.inscheck.util.RussianMetaphone;
import ru.iac.inscheck.ws.model.Query;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

/**
 * Проверка входных данных (Таблица 2 и Таблица 4 постановки) и формирование
 * нормализованных параметров поиска.
 *
 * Нефатальные ошибки реквизитов приводят к тому, что некорректный реквизит
 * считается незаполненным (примечания к Таблице 4): такой реквизит не попадает
 * в SearchParams и по нему поиск не ведётся.
 */
@Component
public class InputValidator {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Pattern P_DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private static final Pattern P_NUM = Pattern.compile("\\d+");
    private static final Pattern P_SNILS = Pattern.compile("\\d{3}-\\d{3}-\\d{3} \\d{2}");
    // ФИО: буквы русского алфавита, цифры, пробел, «-», «.» (Таблица 2 / контроль 101)
    private static final Pattern P_FIO = Pattern.compile("[А-ЯЁа-яё0-9 .\\-]*");

    private final InsCheckDao dao;

    public InputValidator(InsCheckDao dao) {
        this.dao = dao;
    }

    /**
     * Проверка данных (контроли 100–111, аналог пакета inscheck.checkIn).
     * Структурные ошибки 3/4 (формат/значность/обязательность) здесь НЕ проверяются —
     * их выполняет structuralCheck(...) до вызова этого метода.
     */
    public Validation validate(Query q) {
        Validation v = new Validation();
        SearchParams sp = v.getParams();

        // --- Даты и период (контроль 100, фатальный). Формат уже проверен структурно. ---
        LocalDate d1 = parseDate(q.getDate1());
        LocalDate d2 = parseDate(q.getDate2());
        if (d1 != null && d2 != null) {
            if (d1.isAfter(d2) || d2.isAfter(LocalDate.now())) {
                v.add(ErrCode.E100);
            }
        }

        // --- Тип организации (контроль 110) ---
        String typeOrg = trim(q.getType_org());
        if (notBlank(typeOrg) && !typeOrg.matches("[123]")) {
            v.add(ErrCode.E110);
        }

        // --- Код организации (контроль 111): code_org среди актуальных (dbegin..dend)
        // кодов SpMO/SpSMO (type_org 1/2) либо = 0 для type_org=3. ---
        String codeOrg = trim(q.getCode_org());
        if (notBlank(codeOrg) && notBlank(typeOrg) && typeOrg.matches("[123]")
                && !dao.orgExists(typeOrg, codeOrg)) {
            v.add(ErrCode.E111);
        }

        // --- Пол (контроль 102) ---
        String w = trim(q.getW());
        if (notBlank(w)) {
            if (w.matches("[12]")) {
                sp.setW(Integer.valueOf(w));
            } else {
                v.add(ErrCode.E102); // некорректный пол — реквизит считается незаполненным
            }
        }

        // --- Дата рождения (контроль 103) ---
        LocalDate dr = parseDate(q.getDr());
        if (notBlank(q.getDr())) {
            LocalDate upper = d2 != null ? d2 : LocalDate.now();
            LocalDate lower = LocalDate.now().minusYears(150);
            if (dr == null || dr.isBefore(lower) || dr.isAfter(upper)) {
                v.add(ErrCode.E103);
                dr = null; // реквизит считается незаполненным
            }
        }
        if (dr != null) {
            sp.setDr(dr.format(ISO));
        }

        // --- Полис (контроли 104, 105) ---
        validatePolis(q, v, sp);

        // --- Документ (контроль 106) ---
        validateDoc(q, v, sp);

        // --- СНИЛС (контроль 107) ---
        String snils = trim(q.getSnils());
        if (notBlank(snils)) {
            if (!P_SNILS.matcher(snils).matches()) {
                v.add(ErrCode.E107);
            } else {
                sp.setSnils(snils);
            }
        }

        // --- ФИО (контроль 101) ---
        validateFio(q, v, sp);

        // --- Место рождения ---
        String mr = trim(q.getMr());
        if (notBlank(mr)) {
            sp.setMr(mr.toUpperCase());
        }

        return v;
    }

    /**
     * Структурная проверка входа (порт GIPSV2_checkInParam из InsCheck.vb):
     * выполняется ДО пакета, «fail-fast» — возвращает первую найденную ошибку
     * (код 4 «не заполнено обязательное» или 3 «неверный формат/значность») с именем
     * тега, либо null. Данные-контроли (100–111) при этом не выполняются.
     */
    public StructError structuralCheck(Query q) {
        if (q == null) {
            return new StructError(4, "nrec");
        }
        String nrec = nz(q.getNrec());
        if (nrec.isEmpty()) return new StructError(4, "nrec");
        if (nrec.length() > 32) return new StructError(3, "nrec");

        String date1 = nz(q.getDate1());
        if (date1.trim().isEmpty()) return new StructError(4, "date1");
        if (!P_DATE.matcher(date1).find()) return new StructError(3, "date1");

        String date2 = nz(q.getDate2());
        if (date2.trim().isEmpty()) return new StructError(4, "date2");
        if (!P_DATE.matcher(date2).find()) return new StructError(3, "date2");

        String typeOrg = nz(q.getType_org());
        if (typeOrg.isEmpty()) return new StructError(4, "type_org");
        if (!isNum(typeOrg) || typeOrg.length() > 1) return new StructError(3, "type_org");

        String codeOrg = nz(q.getCode_org());
        if (codeOrg.isEmpty()) return new StructError(4, "code_org");
        if (!isNum(codeOrg) || codeOrg.length() > 4) return new StructError(3, "code_org");

        String dr = nz(q.getDr());
        if (!dr.trim().isEmpty() && !P_DATE.matcher(dr).find()) return new StructError(3, "dr");

        String w = nz(q.getW());
        if (!w.isEmpty() && (!isNum(w) || w.length() > 1)) return new StructError(3, "w");

        String vpolis = nz(q.getVpolis());
        if (!vpolis.isEmpty() && (!isNum(vpolis) || vpolis.length() > 1)) return new StructError(3, "vpolis");

        String npolis = nz(q.getNpolis());
        if (!npolis.isEmpty() && (!isNum(npolis) || npolis.length() > 16)) return new StructError(3, "npolis");

        String doctype = nz(q.getDoctype());
        if (!doctype.isEmpty() && (!isNum(doctype) || doctype.length() > 16)) return new StructError(3, "doctype");

        if (nz(q.getFam()).length() > 40) return new StructError(3, "fam");
        if (nz(q.getIm()).length() > 40) return new StructError(3, "im");
        if (nz(q.getOt()).length() > 40) return new StructError(3, "ot");
        if (nz(q.getDocser()).length() > 10) return new StructError(3, "docser");
        if (nz(q.getDocnum()).length() > 20) return new StructError(3, "docnum");
        if (nz(q.getSnils()).length() > 14) return new StructError(3, "snils");
        if (nz(q.getMr()).length() > 160) return new StructError(3, "mr");

        return null;
    }

    /** Структурная ошибка входа: код (3/4) и имя тега; текст — как в старом сервисе. */
    public record StructError(int code, String tag) {
        public String text() {
            String prefix = code == 3
                    ? "Неверный формат или значность реквизита: "
                    : "Не все обязательные поля заполнены: ";
            return prefix + tag;
        }
    }

    private void validatePolis(Query q, Validation v, SearchParams sp) {
        String vpolis = trim(q.getVpolis());
        String npolis = trim(q.getNpolis());
        boolean anyFilled = notBlank(vpolis) || notBlank(npolis);
        if (!anyFilled) {
            return;
        }
        // контроль 104: тип полиса
        if (notBlank(vpolis) && !vpolis.matches("[123]")) {
            v.add(ErrCode.E104);
            return; // Vpolis,Npolis считаются незаполненными
        }
        // контроль 105: оба поля должны быть заполнены и пройти проверку значности
        boolean ok = notBlank(vpolis) && notBlank(npolis) && P_NUM.matcher(npolis).matches();
        if (ok && "2".equals(vpolis)) {
            ok = npolis.length() == 9 && stripLeadingZeros(npolis).length() >= 7;
        } else if (ok && "3".equals(vpolis)) {
            ok = npolis.length() == 16 && stripLeadingZeros(npolis).length() >= 15;
        }
        if (!ok) {
            v.add(ErrCode.E105);
            return; // Vpolis,Npolis считаются незаполненными
        }
        sp.setVpolis(Integer.valueOf(vpolis));
        sp.setNpolis(npolis);
    }

    private void validateDoc(Query q, Validation v, SearchParams sp) {
        String doctype = trim(q.getDoctype());
        String docser = trim(q.getDocser());
        String docnum = trim(q.getDocnum());
        boolean anyFilled = notBlank(doctype) || notBlank(docser) || notBlank(docnum);
        if (!anyFilled) {
            return;
        }
        // контроль 106: при заполнении документа обязательны Doctype и Docnum,
        // Doctype должен быть числом из справочника SPDOCPER.
        boolean ok = notBlank(doctype) && notBlank(docnum)
                && P_NUM.matcher(doctype).matches()
                && dao.docTypeExists(Integer.valueOf(doctype));
        if (!ok) {
            v.add(ErrCode.E106);
            return; // DocType,Docser,DocNum считаются незаполненными
        }
        sp.setDoctype(Integer.valueOf(doctype));
        sp.setDocser(docser == null ? null : docser.toUpperCase());
        sp.setDocnum(docnum);
    }

    private void validateFio(Query q, Validation v, SearchParams sp) {
        String fam = upperOrNull(q.getFam());
        String im = upperOrNull(q.getIm());
        String ot = upperOrNull(q.getOt());

        boolean badChars = !fioOk(fam) || !fioOk(im) || !fioOk(ot);

        // Незаполненные значения считать = «-»
        String f = blankToDash(fam);
        String i = blankToDash(im);
        String o = blankToDash(ot);

        // Не более 1 элемента из (FAM,IM,OT) = «-»
        long dashCount = (f.equals("-") ? 1 : 0) + (i.equals("-") ? 1 : 0) + (o.equals("-") ? 1 : 0);

        if (badChars || dashCount > 1) {
            v.add(ErrCode.E101);
            return; // при ошибке реквизиты ФИО считаются незаполненными
        }

        sp.setFam(f);
        sp.setIm(i);
        sp.setOt(o);
        sp.setMetaFam(RussianMetaphone.encode(f));
        sp.setMetaIm(RussianMetaphone.encode(i));
        sp.setMetaOt(RussianMetaphone.encode(o));
        sp.setFirstIm(firstLetter(i));
        sp.setFirstOt(firstLetter(o));
    }

    // ===== helpers =====

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
    private static boolean notBlank(String s) { return !isBlank(s); }
    private static String trim(String s) { return s == null ? null : s.trim(); }
    private static String nz(String s) { return s == null ? "" : s; }
    private static boolean isNum(String s) { return s.matches("[0-9]*"); }

    private static String upperOrNull(String s) {
        String t = trim(s);
        return t == null ? null : t.toUpperCase();
    }

    private static boolean fioOk(String s) {
        return s == null || s.isEmpty() || P_FIO.matcher(s).matches();
    }

    private static String blankToDash(String s) {
        return (s == null || s.isEmpty()) ? "-" : s;
    }

    private static String firstLetter(String s) {
        return (s == null || s.isEmpty() || s.equals("-")) ? null : s.substring(0, 1);
    }

    private static String stripLeadingZeros(String s) {
        return s.replaceFirst("^0+", "");
    }

    private static LocalDate parseDate(String s) {
        String t = trim(s);
        if (t == null || t.isEmpty() || !P_DATE.matcher(t).matches()) {
            return null;
        }
        try {
            return LocalDate.parse(t, ISO);
        } catch (Exception e) {
            return null;
        }
    }
}
