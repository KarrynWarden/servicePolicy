package ru.iac.inscheck.dao;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.iac.inscheck.util.SqlLoader;

import java.sql.Types;

/**
 * DAO журнала операций (таблица INSCHECKLOG), раздел 3.1 постановки.
 * Весь доступ к БД — только здесь (правило «обращение к БД строго из DAO-слоя»).
 */
@Repository
public class InsCheckLogDao {

    private final NamedParameterJdbcTemplate jdbc;
    private final SqlLoader sql;

    public InsCheckLogDao(NamedParameterJdbcTemplate jdbc, SqlLoader sql) {
        this.jdbc = jdbc;
        this.sql = sql;
    }

    /**
     * Добавляет запись в журнал InscheckLog.
     *
     * @param inpar  параметры запроса «тег=значение» через «;»
     * @param err    коды возвращаемых ошибок через «,»
     * @param iptfoms IP, на который сделан запрос (для 10.0.14.46), иначе null
     */
    public void insertLog(String method, String login, String ip, String inpar,
                          String err, String typeOrg, String codeOrg, String iptfoms) {
        MapSqlParameterSource p = new MapSqlParameterSource();
        p.addValue("method", method, Types.VARCHAR);
        p.addValue("login", login, Types.VARCHAR);
        p.addValue("ip", ip, Types.VARCHAR);
        p.addValue("inpar", inpar, Types.VARCHAR);
        p.addValue("err", err, Types.VARCHAR);
        p.addValue("typeorg", typeOrg, Types.VARCHAR);
        p.addValue("codeorg", codeOrg, Types.VARCHAR);
        p.addValue("iptfoms", iptfoms, Types.VARCHAR);
        jdbc.update(sql.getSql("log/inschecklog_ins.sql"), p);
    }
}
