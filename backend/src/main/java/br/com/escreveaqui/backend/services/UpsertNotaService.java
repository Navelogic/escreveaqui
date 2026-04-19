package br.com.escreveaqui.backend.services;

import br.com.escreveaqui.backend.models.Nota;
import br.com.escreveaqui.backend.repositories.NotaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpsertNotaService {

    private static final Pattern ACCENT_PATTERN =
            Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    private final NotaRepository notaRepository;

    @CacheEvict(value = "notas", key = "#slug")
    @Transactional
    public void execute(String slug, String content) {
        String safeSlug = makeSlug(slug);
        boolean isNew = notaRepository.findBySlug(safeSlug).isEmpty();

        Nota nota = notaRepository.findBySlug(safeSlug)
                .orElseGet(() -> Nota.builder().slug(safeSlug).build());
        nota.setContent(content);
        notaRepository.save(nota);

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
