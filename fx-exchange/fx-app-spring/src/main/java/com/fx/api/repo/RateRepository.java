package com.fx.api.repo;

import com.fx.api.model.Rate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

/** Deck 03 Spring JDBC — JdbcTemplate over the fxdb schema Liquibase now owns. */
@Repository
public class RateRepository {

    private final JdbcTemplate jdbc;

    private static final RowMapper<Rate> MAPPER = (rs, n) -> {
        Timestamp captured = rs.getTimestamp("captured_at");
        return new Rate(
                rs.getInt("id"), rs.getString("base_code"), rs.getString("quote_code"),
                rs.getDouble("rate"), rs.getDate("rate_date").toLocalDate(),
                captured == null ? null : captured.toLocalDateTime());
    };

    public RateRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }   // constructor injection

    /**
     * The newest row PER PAIR.
     *
     * Until Activity 5 this was "every row on the newest rate_date", which worked only
     * because the seed had one row per pair per day. The live feed writes many rows per
     * pair per day, so the question had to change: not "what is the latest day?" but
     * "what is the latest row for each pair?".
     *
     * On the seeded data the answer is identical — 10 rows, EUR/USD 1.0818.
     */
    public List<Rate> findLatest() {
        return jdbc.query("""
            SELECT r.* FROM fx_rate r
            WHERE r.id = (SELECT r2.id FROM fx_rate r2
                          WHERE r2.base_code = r.base_code AND r2.quote_code = r.quote_code
                          ORDER BY r2.captured_at DESC, r2.id DESC
                          LIMIT 1)
            ORDER BY r.base_code, r.quote_code""", MAPPER);
    }

    public Optional<Rate> findLatestForPair(String base, String quote) {
        List<Rate> rows = jdbc.query("""
            SELECT * FROM fx_rate WHERE base_code=? AND quote_code=?
            ORDER BY captured_at DESC, id DESC LIMIT 1""", MAPPER, base, quote);
        return rows.stream().findFirst();
    }

    public int insert(String base, String quote, double rate) {
        return jdbc.update("""
            INSERT INTO fx_rate (base_code, quote_code, rate, rate_date, captured_at)
            VALUES (?,?,?,CURDATE(),CURRENT_TIMESTAMP(3))""",
                base, quote, rate);
    }

    /** One tick from the upstream feed. Same insert — named for what calls it. */
    public int insertTick(String base, String quote, double rate) {
        return insert(base, quote, rate);
    }
}
