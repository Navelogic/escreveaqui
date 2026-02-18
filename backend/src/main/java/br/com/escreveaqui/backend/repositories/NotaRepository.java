package br.com.escreveaqui.backend.repositories;

import br.com.escreveaqui.backend.models.Nota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotaRepository extends JpaRepository<Nota, UUID> {

    @Transactional(readOnly = true)
    Optional<Nota> findBySlug(String slug);

    @Modifying
    @Transactional
    @Query("DELETE FROM Nota n WHERE n.updatedAt < :dateTime")
    int deleteOldNotes(@Param("dateTime") OffsetDateTime dateTime);

    boolean existsBySlug(String slug);


}
