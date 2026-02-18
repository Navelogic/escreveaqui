package br.com.escreveaqui.backend.services;

import br.com.escreveaqui.backend.models.Nota;
import br.com.escreveaqui.backend.repositories.NotaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpsertNotaService {

    private final NotaRepository notaRepository;

    @Transactional
    public void execute(String slug, String content) {

        Nota nota = notaRepository
                .findBySlug(slug)
                .orElseGet(() -> Nota.builder()
                        .slug(slug)
                        .build());

        nota.setContent(content);

        notaRepository.save(nota);
    }
}
