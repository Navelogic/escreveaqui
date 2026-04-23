package br.com.escreveaqui.backend.services;

import br.com.escreveaqui.backend.repositories.NotaRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Slf4j
@Service
public class DeleteNotaService {

    private final NotaRepository notaRepository;
    private final Counter deletedCounter;

    public DeleteNotaService(NotaRepository notaRepository, MeterRegistry registry) {
        this.notaRepository = notaRepository;
        this.deletedCounter = Counter.builder("notes.deleted")
                .description("Notas removidas pelo job de limpeza")
                .register(registry);
    }

    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    @CacheEvict(value = "notas", allEntries = true)
    public void execute() {
        OffsetDateTime limitDate = OffsetDateTime.now().minusDays(30);
        log.info("Iniciando limpeza de notas inativas antes de {}", limitDate);

        int deleted = notaRepository.deleteOldNotes(limitDate);
        deletedCounter.increment(deleted);

        log.info("Limpeza concluída: {} nota(s) removida(s)", deleted);
    }
}
