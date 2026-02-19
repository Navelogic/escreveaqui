package br.com.escreveaqui.backend.services;

import br.com.escreveaqui.backend.models.Nota;
import br.com.escreveaqui.backend.repositories.NotaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class UpsertNotaService {

    private final NotaRepository notaRepository;

    @Transactional
    public void execute(String slug, String content) {
        String safeSlug = makeSlug(slug);
        Nota nota = notaRepository.findBySlug(safeSlug)
                .orElseGet(() -> Nota.builder().slug(safeSlug).build());
        nota.setContent(content);
        notaRepository.save(nota);
    }

    private String makeSlug(String input) {
        if (input == null) return "";

        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        Pattern accentPattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String out = accentPattern.matcher(normalized).replaceAll("");

        return out.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }
}