package br.com.escreveaqui.backend.services;

import br.com.escreveaqui.backend.dtos.NotaResponseDTO;
import br.com.escreveaqui.backend.models.Nota;
import br.com.escreveaqui.backend.repositories.NotaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class ReadNotaService {

    private final NotaRepository notaRepository;

    @Transactional(readOnly = true)
    public NotaResponseDTO execute(String slug) {
        return notaRepository.findBySlug(slug)
                .map(nota -> new NotaResponseDTO(
                        nota.getSlug(),
                        nota.getContent(),
                        nota.getUpdatedAt()))
                .orElse(new NotaResponseDTO(slug, "", OffsetDateTime.now()));
    }
}
