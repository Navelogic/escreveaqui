package br.com.escreveaqui.backend.services;

import br.com.escreveaqui.backend.repositories.NotaRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.regex.Pattern;

@Slf4j
@Service
public class UpsertNotaService {

    private static final Pattern ACCENT_PATTERN =
            Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    private final NotaRepository notaRepository;
    private final Counter createCounter;
    private final Counter updateCounter;

    public UpsertNotaService(NotaRepository notaRepository, MeterRegistry registry) {
        this.notaRepository = notaRepository;
        this.createCounter = Counter.builder("notes.upsert")
                .tag("operation", "create")
                .description("Notas criadas")
                .register(registry);
        this.updateCounter = Counter.builder("notes.upsert")
                .tag("operation", "update")
                .description("Notas atualizadas")
                .register(registry);
    }

    @CacheEvict(value = "notas", key = "#slug")
    public void execute(String slug, String content) {
        String safeSlug = makeSlug(slug);

        boolean isNew = notaRepository.upsert(safeSlug, content);

        if (isNew) createCounter.increment();
        else updateCounter.increment();
        log.debug("{} nota: slug='{}'", isNew ? "Criada" : "Atualizada", safeSlug);
    }

    private String makeSlug(String input) {
        if (input == null) return "";

        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        return ACCENT_PATTERN.matcher(normalized).replaceAll("")
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }
}
