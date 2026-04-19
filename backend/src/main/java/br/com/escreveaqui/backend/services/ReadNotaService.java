package br.com.escreveaqui.backend.services;

import br.com.escreveaqui.backend.dtos.NotaResponseDTO;
import br.com.escreveaqui.backend.repositories.NotaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReadNotaService {

    private final NotaRepository notaRepository;

    @Cacheable(value = "notas", key = "#slug")
    @Transactional(readOnly = true)
    public NotaResponseDTO execute(String slug) {
        return notaRepository.findBySlug(slug)
                .map(nota -> {
                    log.debug("Nota encontrada: slug='{}'", slug);
                    return new NotaResponseDTO(nota.getSlug(), nota.getContent(), nota.getUpdatedAt());
                })
                .orElseGet(() -> {
                    log.debug("Nota não encontrada, retornando vazia: slug='{}'", slug);
                    return new NotaResponseDTO(slug, "", OffsetDateTime.now());
                });
    }
}
