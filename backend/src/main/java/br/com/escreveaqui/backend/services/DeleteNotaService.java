package br.com.escreveaqui.backend.services;

import br.com.escreveaqui.backend.repositories.NotaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeleteNotaService {

    private final NotaRepository notaRepository;

    // Roda às 3 da manhã todos os dias
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void execute() {
        var limitDate = java.time.OffsetDateTime.now().minusDays(30);
        notaRepository.deleteOldNotes(limitDate);
        // No futuro, gostaria de começar a logar as coisas aqui.
    }
}
