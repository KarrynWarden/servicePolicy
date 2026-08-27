package ru.iac.inscheck.dao;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.iac.inscheck.model.Candidate;
import ru.iac.inscheck.model.IPerson;
import ru.iac.inscheck.model.IPrkDept;
import ru.iac.inscheck.model.SearchAlgorithm;
import ru.iac.inscheck.model.SearchParams;
import ru.iac.inscheck.util.SqlLoader;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Основной DAO сервиса: справочные проверки, поиск СП по алгоритмам Таблицы 1,
 * выборка комплекта полисов (СК*), прикрепления и признаков диспансеризации.
 *
 * Весь SQL вынесен в отдельные файлы (sql/...), доступ к БД — только из этого слоя.
 */
@Repository
public class InsCheckDao {

    private final NamedParameterJdbcTemplate jdbc;
    private final SqlLoader sql;

    public InsCheckDao(NamedParameterJdbcTemplate jdbc, SqlLoader sql) {
        this.jdbc = jdbc;
        this.sql = sql;
    }

    // ===== Справочные проверки =====

    /**
     * Контроль 111: код организации присутствует среди кодов SpMO (type_org=1) /
     * SpSMO (type_org=2), действовавших в период запроса [date1; date2], либо = 0
     * для type_org=3. Период запроса, а не текущая дата: код мог быть закрыт позже
     * даты обращения и всё равно был верным на момент запроса.
     */
    public boolean orgExists(String typeOrg, String codeOrg, LocalDate date1, LocalDate date2) {
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("type_org", typeOrg)
                .addValue("code_org", codeOrg)
                .addValue("date1", date1)
                .addValue("date2", date2);
        Integer cnt = jdbc.queryForObject(sql.getSql("ref/org_check.sql"), p, Integer.class);
        return cnt != null && cnt > 0;
    }

    /** Проверка типа документа по справочнику SPDOCPER (ошибка 106). */
    public boolean docTypeExists(Integer doctype) {
        Integer cnt = jdbc.queryForObject(sql.getSql("ref/doctype_check.sql"),
                new MapSqlParameterSource("doctype", doctype), Integer.class);
        return cnt != null && cnt > 0;
    }

    /**
     * Контроль 206: документ ПЗСК* соответствует запросу. Документы ЗЛ хранятся в IDOC
     * (связь по IDROW, TYPEROW in (1,2)). true, если 206 НЕ нужен: есть совместимый
     * документ (null-терпимо, как <> с NULL в пакете) либо документов нет вовсе.
     */
    public boolean docMatches(Long idrow, Integer doctype, String docser, String docnum) {
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("idrow", idrow)
                .addValue("doctype", doctype)
                .addValue("docser", docser)
                .addValue("docnum", docnum);
        Integer cnt = jdbc.queryForObject(sql.getSql("ref/idoc_check.sql"), p, Integer.class);
        return cnt != null && cnt > 0;
    }

    /** Наличие номера полиса в Регистре (ошибка 108). */
    public boolean npolisExists(Integer vpolis, String npolis) {
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("vpolis", vpolis)
                .addValue("npolis", npolis);
        Integer cnt = jdbc.queryForObject(sql.getSql("ref/npolis_check.sql"), p, Integer.class);
        return cnt != null && cnt > 0;
    }

    // ===== Поиск СП =====

    /** Выполняет один алгоритм поиска (Таблица 1 / раздел H). Возвращает найденные СК. */
    public List<Candidate> search(SearchAlgorithm algorithm, SearchParams sp) {
        return jdbc.query(sql.getSql(algorithm.getSqlFile()), searchParams(sp),
                (rs, n) -> new Candidate(rs.getLong("id"), longOrNull(rs, "idmain")));
    }

    private MapSqlParameterSource searchParams(SearchParams sp) {
        return new MapSqlParameterSource()
                .addValue("fam", sp.getFam())
                .addValue("im", sp.getIm())
                .addValue("ot", sp.getOt())
                .addValue("meta_fam", sp.getMetaFam())
                .addValue("meta_im", sp.getMetaIm())
                .addValue("meta_ot", sp.getMetaOt())
                .addValue("first_im", sp.getFirstIm())
                .addValue("first_ot", sp.getFirstOt())
                .addValue("dr", sp.getDr())
                .addValue("mr", sp.getMr())
                .addValue("vpolis", sp.getVpolis())
                .addValue("npolis", sp.getNpolis())
                .addValue("doctype", sp.getDoctype())
                .addValue("docser", sp.getDocser())
                .addValue("docnum", sp.getDocnum())
                .addValue("snils", sp.getSnils())
                .addValue("sim", sp.getSimilarity());
    }

    // ===== Комплект полисов (СК*), прикрепление, признаки =====

    /**
     * Все записи комплекта полисов (СК* = записи с одинаковым IDMain, п.4.4).
     * Если idmain не задан — берётся одиночная запись по id.
     */
    public List<IPerson> findSkMembers(Long idmain, long id) {
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("idmain", idmain)
                .addValue("id", id);
        return jdbc.query(sql.getSql("res/sk_members_sel.sql"), p, PERSON_MAPPER);
    }

    /** Прикрепление ЗЛ для ПЗСК* (п.5.1). null, если актуального прикрепления нет. */
    public IPrkDept findPrk(long personId, LocalDate date1, LocalDate date2) {
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("id", personId)
                .addValue("date1", date1)
                .addValue("date2", date2);
        List<IPrkDept> rows = jdbc.query(sql.getSql("res/prk_sel.sql"), p, PRK_MAPPER);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /** Диагностика: все строки IPRKDEPT по id (без фильтра дат) — для отладки контроля 305. */
    public List<IPrkDept> findAllPrk(long personId) {
        return jdbc.query(sql.getSql("res/prk_debug_sel.sql"),
                new MapSqlParameterSource("id", personId), PRK_MAPPER);
    }

    /**
     * Сходство мест рождения по jaro_winkler (контроль 208). Возвращает значение 0..1,
     * как Oracle utl_match.jaro_winkler(upper(mr1), upper(mr2)). Сравнивается с порогом 0.8.
     */
    public double mrSimilarity(String mr1, String mr2) {
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("mr1", mr1)
                .addValue("mr2", mr2);
        Double sim = jdbc.queryForObject(sql.getSql("ref/mr_similar.sql"), p, Double.class);
        return sim == null ? 0.0 : sim;
    }

    /**
     * Признак прохождения мероприятия по MEDREE_PRDISP: groupcode 1 — диспансеризация,
     * 2 — профосмотр, 3 — Центр здоровья, за указанный год.
     */
    public boolean hasDisp(long personId, int groupcode, int year) {
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("id", personId)
                .addValue("groupcode", groupcode)
                .addValue("yr", year);
        Integer cnt = jdbc.queryForObject(sql.getSql("res/event_check.sql"), p, Integer.class);
        return cnt != null && cnt > 0;
    }

    // ===== RowMappers =====

    private static final RowMapper<IPerson> PERSON_MAPPER = (rs, n) -> {
        IPerson p = new IPerson();
        p.setId(rs.getLong("id"));
        p.setIdrow(longOrNull(rs, "idrow"));
        p.setIdmain(longOrNull(rs, "idmain"));
        p.setSmo(intOrNull(rs, "smo"));
        p.setVpolis(intOrNull(rs, "vpolis"));
        p.setFpolis(intOrNull(rs, "fpolis"));
        p.setNpolis(rs.getString("npolis"));
        p.setEnp(rs.getString("enp"));
        p.setFam(rs.getString("fam"));
        p.setIm(rs.getString("im"));
        p.setOt(rs.getString("ot"));
        p.setW(intOrNull(rs, "w"));
        p.setDr(getDate(rs, "dr"));
        p.setMr(rs.getString("mr"));
        p.setSs(rs.getString("ss"));
        p.setDvizit(getDate(rs, "dvizit"));
        p.setDbeg(getDate(rs, "dbeg"));
        p.setDend(getDate(rs, "dend"));
        p.setReason(intOrNull(rs, "reason"));
        p.setDdeath(getDate(rs, "ddeath"));
        return p;
    };

    private static final RowMapper<IPrkDept> PRK_MAPPER = (rs, n) -> {
        IPrkDept p = new IPrkDept();
        p.setId(rs.getLong("id"));
        p.setDbeg(getDate(rs, "dbeg"));
        p.setDend(getDate(rs, "dend"));
        p.setTypeprk(intOrNull(rs, "typeprk"));
        p.setMo(intOrNull(rs, "mo"));
        p.setOtdel(rs.getString("otdel"));
        p.setDept(intOrNull(rs, "dept"));
        p.setSubdept(intOrNull(rs, "subdept"));
        return p;
    };

    private static LocalDate getDate(ResultSet rs, String col) throws SQLException {
        java.sql.Date d = rs.getDate(col);
        return d == null ? null : d.toLocalDate();
    }

    /** Числовые колонки БД (в т.ч. numeric/BigDecimal) читаем через Number — null-безопасно. */
    private static Long longOrNull(ResultSet rs, String col) throws SQLException {
        Object o = rs.getObject(col);
        return o == null ? null : ((Number) o).longValue();
    }

    private static Integer intOrNull(ResultSet rs, String col) throws SQLException {
        Object o = rs.getObject(col);
        return o == null ? null : ((Number) o).intValue();
    }
}
