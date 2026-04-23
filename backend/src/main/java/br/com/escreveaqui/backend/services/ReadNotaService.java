package br.com.escreveaqui.backend.services;

import br.com.escreveaqui.backend.dtos.NotaResponseDTO;
import br.com.escreveaqui.backend.repositories.NotaRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Slf4j
@Service
public class ReadNotaService {

    private final NotaRepository notaRepository;
    private final Counter hitCounter;
    private final Counter missCounter;

    public ReadNotaService(NotaRepository notaRepository, MeterRegistry registry) {
        this.notaRepository = notaRepository;
        this.hitCounter  = Counter.builder("notes.read")
                .tag("result", "hit")
                .description("Notas encontradas no banco")
                .register(registry);
        this.missCounter = Counter.builder("notes.read")
                .tag("result", "miss")
                .description("Slugs acessados sem nota existente")
                .register(registry);
    }

    @Cacheable(value = "notas", key = "#slug")
    @Transactional(readOnly = true)
    public NotaResponseDTO execute(String slug) {
        return notaRepository.findBySlug(slug)
                .map(nota -> {
                    hitCounter.increment();
                    log.debug("Nota encontrada: slug='{}'", slug);
                    return new NotaResponseDTO(nota.getSlug(), nota.getContent(), nota.getUpdatedAt());
                })
                .orElseGet(() -> {
                    missCounter.increment();
                    log.debug("Nota não encontrada, retornando vazia: slug='{}'", slug);
                    return new NotaResponseDTO(slug, "", OffsetDateTime.now());
                });
    }
}
