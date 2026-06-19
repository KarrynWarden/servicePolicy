package ru.iac.inscheck.dao;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.iac.inscheck.util.SqlLoader;

import java.time.LocalDate;

/**
 * DAO счётчика запросов по IP (таблица INSCHECKLIMIT), раздел 3.2 постановки.
 */
@Repository
public class InsCheckLimitDao {

    private static final int DEFAULT_LIMIT = 1000;

    private final NamedParameterJdbcTemplate jdbc;
    private final SqlLoader sql;

    public InsCheckLimitDao(NamedParameterJdbcTemplate jdbc, SqlLoader sql) {
        this.jdbc = jdbc;
        this.sql = sql;
    }

    /** Текущее состояние лимита по IP (dreq, kol, limit) либо null, если записи нет. */
    public LimitRow select(String ip) {
        try {
            return jdbc.queryForObject(
                    sql.getSql("limit/inschecklimit_sel.sql"),
                    new MapSqlParameterSource("ip", ip),
                    (rs, n) -> new LimitRow(
                            rs.getObject("dreq", LocalDate.class),
                            rs.getInt("kol"),
                            rs.getInt("limit")));
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    /** Добавляет запись лимита: KOL = 1, LIMIT = 1000 (п.3.2.2). */
    public void insert(String ip) {
        MapSqlParameterSource p = new MapSqlParameterSource();
        p.addValue("ip", ip);
        p.addValue("dreq", LocalDate.now());
        p.addValue("kol", 1);
        p.addValue("limit", DEFAULT_LIMIT);
        jdbc.update(sql.getSql("limit/inschecklimit_ins.sql"), p);
    }

    /**
     * Изменяет запись лимита: если DREQ &lt; тек.даты, то KOL = 1 (новый день),
     * иначе KOL = KOL + 1; DREQ = тек.дата (п.3.2.2).
     */
    public void update(String ip) {
        MapSqlParameterSource p = new MapSqlParameterSource();
        p.addValue("ip", ip);
        p.addValue("dreq", LocalDate.now());
        jdbc.update(sql.getSql("limit/inschecklimit_upd.sql"), p);
    }

    public record LimitRow(LocalDate dreq, int kol, int limit) {
        /** Лимит исчерпан, если запрос в тот же день и KOL уже достиг LIMIT (ошибка 5). */
        public boolean exceededToday() {
            return dreq != null && dreq.isEqual(LocalDate.now()) && kol >= limit;
        }
    }
}
