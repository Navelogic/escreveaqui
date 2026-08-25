package br.com.escreveaqui.backend.services;

import br.com.escreveaqui.backend.repositories.NotaRepository;
import br.com.escreveaqui.backend.utils.SlugUtils;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UpsertNotaService {

    private final NotaRepository notaRepository;
    private final SseService sseService;
    private final Counter createCounter;
    private final Counter updateCounter;

    public UpsertNotaService(NotaRepository notaRepository, SseService sseService, MeterRegistry registry) {
        this.notaRepository = notaRepository;
        this.sseService = sseService;
        this.createCounter = Counter.builder("notes.upsert")
                .tag("operation", "create")
                .description("Notas criadas")
                .register(registry);
        this.updateCounter = Counter.builder("notes.upsert")
                .tag("operation", "update")
                .description("Notas atualizadas")
                .register(registry);
    }

    @CacheEvict(value = "notas", key = "T(br.com.escreveaqui.backend.utils.SlugUtils).format(#slug)")
    public void execute(String slug, String content) {
        String safeSlug = SlugUtils.format(slug);

        boolean isNew = notaRepository.upsert(safeSlug, content);
        sseService.notify(safeSlug, content);

        if (isNew) createCounter.increment();
        else updateCounter.increment();
        log.debug("{} nota: slug='{}'", isNew ? "Criada" : "Atualizada", safeSlug);
    }
}
