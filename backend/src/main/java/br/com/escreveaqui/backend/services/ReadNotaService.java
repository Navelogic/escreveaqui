package br.com.escreveaqui.backend.services;

import br.com.escreveaqui.backend.models.Nota;
import br.com.escreveaqui.backend.repositories.NotaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReadNotaService {

    private final NotaRepository notaRepository;

    @Transactional(readOnly = true)
    public String execute(String slug) {
        return notaRepository.findBySlug(slug)
                .map(Nota::getContent)
                .orElse("");
    }
}
