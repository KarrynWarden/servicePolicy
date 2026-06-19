package ru.iac.inscheck.model;

/**
 * Алгоритмы поиска СП (Таблица 1 постановки) в порядке применения.
 * Поиск ведётся последовательно до первого найденного факта страхования
 * (п.3.1.4.4, п.4.2.2).
 *
 * Раздел H — новый этап идентификации, добавленный при переносе на новый стек
 * (см. задание): точное совпадение фамилии, имя/отчество по первой букве,
 * ДР и один из идентификаторов (Полис/Документ/СНИЛС). Вставляется между
 * разделами Р и В.
 *
 * Каждому алгоритму соответствует sql-файл sql/search/{name}.sql.
 * Код (code) выдаётся в ответе в теге &lt;alg&gt;.
 */
public enum SearchAlgorithm {

    C01("С01", "search/c01.sql", "схожее(ФИО) + (Др или схожее(Мр)) + Полис"),
    C02("С02", "search/c02.sql", "схожее(ФИО) + (Др или схожее(Мр)) + Документ"),
    C03("С03", "search/c03.sql", "схожее(ФИО) + (Др или схожее(Мр)) + СНИЛС"),

    R01("Р01", "search/r01.sql", "3 из 4х: метафон(ФИО), Др + Полис"),

    H01("H01", "search/h01.sql", "Точный Ф + И/О по первой букве + ДР + Полис"),
    H02("H02", "search/h02.sql", "Точный Ф + И/О по первой букве + ДР + Документ"),
    H03("H03", "search/h03.sql", "Точный Ф + И/О по первой букве + ДР + СНИЛС"),

    V01("В01", "search/v01.sql", "3 из 4х: метафон(ФИО), (Др или схожее(Мр)) + Полис"),
    V02("В02", "search/v02.sql", "3 из 4х: метафон(ФИО), (Др или схожее(Мр)) + Документ"),
    V03("В03", "search/v03.sql", "3 из 4х: метафон(ФИО), (Др или схожее(Мр)) + СНИЛС"),
    V04("В04", "search/v04.sql", "Полис"),
    V05("В05", "search/v05.sql", "ФИО + Др"),
    V06("В06", "search/v06.sql", "метафон(ФИО) + Др");

    private final String code;
    private final String sqlFile;
    private final String description;

    SearchAlgorithm(String code, String sqlFile, String description) {
        this.code = code;
        this.sqlFile = sqlFile;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getSqlFile() { return sqlFile; }
    public String getDescription() { return description; }
}
