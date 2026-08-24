package br.com.escreveaqui.backend.services;

import br.com.escreveaqui.backend.dtos.NotaResponseDTO;
import br.com.escreveaqui.backend.repositories.NotaRepository;
import br.com.escreveaqui.backend.utils.SlugUtils;
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

    @Cacheable(value = "notas", key = "T(br.com.escreveaqui.backend.utils.SlugUtils).format(#slug)")
    @Transactional(readOnly = true)
    public NotaResponseDTO execute(String slug) {
        String safeSlug = SlugUtils.format(slug);
        return notaRepository.findBySlug(safeSlug)
                .map(nota -> {
                    hitCounter.increment();
                    log.debug("Nota encontrada: slug='{}'", safeSlug);
                    return new NotaResponseDTO(nota.slug(), nota.content(), nota.updatedAt());
                })
                .orElseGet(() -> {
                    missCounter.increment();
                    log.debug("Nota não encontrada, retornando vazia: slug='{}'", safeSlug);
                    return new NotaResponseDTO(safeSlug, "", OffsetDateTime.now());
                });
    }
}
