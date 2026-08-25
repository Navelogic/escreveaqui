package br.com.escreveaqui.backend.services;

import br.com.escreveaqui.backend.repositories.NotaRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpsertNotaServiceSseTest {

    @Mock
    private NotaRepository notaRepository;

    @Mock
    private SseService sseService;

    private MeterRegistry meterRegistry;
    private UpsertNotaService upsertNotaService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        upsertNotaService = new UpsertNotaService(notaRepository, sseService, meterRegistry);
    }

    @Test
    @DisplayName("Deve notificar inscritos SSE ao criar ou atualizar uma nota")
    void deveNotificarInscritosSseAoSalvar() {
        String rawSlug = "Nota em Tempo Real";
        String sanitizedSlug = "nota-em-tempo-real";
        String content = "Conteudo transmitido via SSE";

        when(notaRepository.upsert(sanitizedSlug, content)).thenReturn(true);

        upsertNotaService.execute(rawSlug, content);

        verify(notaRepository).upsert(sanitizedSlug, content);
        verify(sseService).notify(sanitizedSlug, content);
    }
}
