package br.com.escreveaqui.backend.services;

import br.com.escreveaqui.backend.repositories.NotaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DeleteNotaService {

    private final NotaRepository notaRepository;

    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void execute() {
        LocalDateTime limitDate = LocalDateTime.now().minusDays(30);
        notaRepository.deleteByUpdatedAtBefore(limitDate);
    }
}
