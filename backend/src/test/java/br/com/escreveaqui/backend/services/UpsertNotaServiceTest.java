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
class UpsertNotaServiceTest {

    @Mock
    private NotaRepository notaRepository;

    @Mock
    private SseService sseService;

    private MeterRegistry meterRegistry;
    private UpsertNotaService upsertNotaService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        upsertNotaService = new UpsertNotaService(notaRepository, sseService, new NotaSecretService(notaRepository), meterRegistry);
    }

    @Test
    @DisplayName("Deve salvar a nota sanitizando o slug informado ao criar nova nota")
    void deveSalvarNotaSanitizandoSlugNovaNota() {
        String rawSlug = "Minha Nova Nota!";
        String sanitizedSlug = "minha-nova-nota";
        String content = "Conteúdo";

        when(notaRepository.upsert(sanitizedSlug, content, null)).thenReturn(true);

        upsertNotaService.execute(rawSlug, content);

        verify(notaRepository).upsert(sanitizedSlug, content, null);
    }

    @Test
    @DisplayName("Deve atualizar a nota sanitizando o slug informado se ela já existir")
    void deveAtualizarNotaSanitizandoSlugExistente() {
        String rawSlug = "Nota Existente";
        String sanitizedSlug = "nota-existente";
        String content = "Conteúdo Atualizado";

        when(notaRepository.upsert(sanitizedSlug, content, null)).thenReturn(false);

        upsertNotaService.execute(rawSlug, content);

        verify(notaRepository).upsert(sanitizedSlug, content, null);
    }
}
