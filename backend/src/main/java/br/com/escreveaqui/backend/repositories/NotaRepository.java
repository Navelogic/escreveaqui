package br.com.escreveaqui.backend.repositories;

import br.com.escreveaqui.backend.models.Nota;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class NotaRepository {

    private static final RowMapper<Nota> NOTA_ROW_MAPPER = (rs, rowNum) -> new Nota(
            rs.getObject("id", UUID.class),
            rs.getString("slug"),
            rs.getString("content"),
            rs.getObject("created_at", OffsetDateTime.class),
            rs.getObject("updated_at", OffsetDateTime.class)
    );

    private final JdbcTemplate jdbcTemplate;

    public NotaRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<Nota> findBySlug(String slug) {
        List<Nota> result = jdbcTemplate.query(
                "SELECT id, slug, content, created_at, updated_at FROM notes WHERE slug = ?",
                NOTA_ROW_MAPPER, slug);
        return result.stream().findFirst();
    }

    public boolean upsert(String slug, String content) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
                INSERT INTO notes (slug, content)
                VALUES (?, ?)
                ON CONFLICT (slug) DO UPDATE
                    SET content = EXCLUDED.content,
                        updated_at = now()
                RETURNING (xmax = 0) AS inserted
                """, Boolean.class, slug, content));
    }

    public int deleteOldNotes(OffsetDateTime cutoff) {
        return jdbcTemplate.update("DELETE FROM notes WHERE updated_at < ?", cutoff);
    }
}
