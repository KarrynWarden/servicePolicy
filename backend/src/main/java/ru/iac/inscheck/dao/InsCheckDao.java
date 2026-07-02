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
     * Контроль 111: код организации присутствует среди актуальных (dbegin..dend) кодов
     * SpMO (type_org=1) / SpSMO (type_org=2), либо = 0 для type_org=3.
     */
    public boolean orgExists(String typeOrg, String codeOrg) {
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("type_org", typeOrg)
                .addValue("code_org", codeOrg);
        Integer cnt = jdbc.queryForObject(sql.getSql("ref/org_check.sql"), p, Integer.class);
        return cnt != null && cnt > 0;
    }

    /** Проверка типа документа по справочнику SPDOCPER (ошибка 106). */
    public boolean docTypeExists(Integer doctype) {
        Integer cnt = jdbc.queryForObject(sql.getSql("ref/doctype_check.sql"),
                new MapSqlParameterSource("doctype", doctype), Integer.class);
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
                (rs, n) -> new Candidate(rs.getLong("id"), (Long) rs.getObject("idmain")));
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
        p.setIdmain((Long) rs.getObject("idmain"));
        p.setSmo((Integer) rs.getObject("smo"));
        p.setVpolis((Integer) rs.getObject("vpolis"));
        p.setFpolis((Integer) rs.getObject("fpolis"));
        p.setNpolis(rs.getString("npolis"));
        p.setEnp(rs.getString("enp"));
        p.setFam(rs.getString("fam"));
        p.setIm(rs.getString("im"));
        p.setOt(rs.getString("ot"));
        p.setW((Integer) rs.getObject("w"));
        p.setDr(getDate(rs, "dr"));
        p.setMr(rs.getString("mr"));
        p.setDoctype((Integer) rs.getObject("doctype"));
        p.setDocser(rs.getString("docser"));
        p.setDocnum(rs.getString("docnum"));
        p.setSs(rs.getString("ss"));
        p.setDvizit(getDate(rs, "dvizit"));
        p.setDbeg(getDate(rs, "dbeg"));
        p.setDend(getDate(rs, "dend"));
        p.setReason((Integer) rs.getObject("reason"));
        p.setDdeath(getDate(rs, "ddeath"));
        return p;
    };

    private static final RowMapper<IPrkDept> PRK_MAPPER = (rs, n) -> {
        IPrkDept p = new IPrkDept();
        p.setId(rs.getLong("id"));
        p.setDbeg(getDate(rs, "dbeg"));
        p.setDend(getDate(rs, "dend"));
        p.setTypeprk((Integer) rs.getObject("typeprk"));
        p.setMo((Integer) rs.getObject("mo"));
        p.setOtdel(rs.getString("otdel"));
        p.setDept((Integer) rs.getObject("dept"));
        p.setSubdept((Integer) rs.getObject("subdept"));
        return p;
    };

    private static LocalDate getDate(ResultSet rs, String col) throws SQLException {
        java.sql.Date d = rs.getDate(col);
        return d == null ? null : d.toLocalDate();
    }
}
