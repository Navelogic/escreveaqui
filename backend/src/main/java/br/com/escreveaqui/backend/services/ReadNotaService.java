package br.com.escreveaqui.backend.services;

import br.com.escreveaqui.backend.dtos.NotaResponseDTO;
import br.com.escreveaqui.backend.repositories.NotaRepository;
import br.com.escreveaqui.backend.utils.SlugUtils;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Slf4j
@Service
public class ReadNotaService {

    private final NotaRepository notaRepository;
    private final NotaSecretService secretService;
    private final Counter hitCounter;
    private final Counter missCounter;

    public ReadNotaService(NotaRepository notaRepository, NotaSecretService secretService, MeterRegistry registry) {
        this.notaRepository = notaRepository;
        this.secretService = secretService;
        this.hitCounter  = Counter.builder("notes.read")
                .tag("result", "hit")
                .description("Notas encontradas no banco")
                .register(registry);
        this.missCounter = Counter.builder("notes.read")
                .tag("result", "miss")
                .description("Slugs acessados sem nota existente")
                .register(registry);
    }

    public NotaResponseDTO execute(String slug) {
        return execute(slug, null);
    }

    @Transactional(readOnly = true)
    public NotaResponseDTO execute(String slug, String secret) {
        String safeSlug = SlugUtils.format(slug);
        return notaRepository.findBySlug(safeSlug)
                .map(nota -> {
                    secretService.check(nota.secretHash(), secret);
                    hitCounter.increment();
                    log.debug("Nota encontrada: slug='{}'", safeSlug);
                    return new NotaResponseDTO(nota.slug(), nota.content(), nota.secretHash() != null, nota.updatedAt());
                })
                .orElseGet(() -> {
                    missCounter.increment();
                    log.debug("Nota não encontrada, retornando vazia: slug='{}'", safeSlug);
                    return new NotaResponseDTO(safeSlug, "", false, OffsetDateTime.now());
                });
    }
}
