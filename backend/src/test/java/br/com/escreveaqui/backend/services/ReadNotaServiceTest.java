package br.com.escreveaqui.backend.services;

import br.com.escreveaqui.backend.dtos.NotaResponseDTO;
import br.com.escreveaqui.backend.models.Nota;
import br.com.escreveaqui.backend.repositories.NotaRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReadNotaServiceTest {

    @Mock
    private NotaRepository notaRepository;

    private MeterRegistry meterRegistry;
    private ReadNotaService readNotaService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        readNotaService = new ReadNotaService(notaRepository, new NotaSecretService(notaRepository), meterRegistry);
    }

    @Test
    @DisplayName("Deve buscar nota sanitizando o slug informado")
    void deveBuscarNotaSanitizandoSlug() {
        String rawSlug = "Minha Nota!";
        String sanitizedSlug = "minha-nota";
        Nota nota = new Nota(UUID.randomUUID(), sanitizedSlug, "Conteúdo da nota", null, OffsetDateTime.now(), OffsetDateTime.now());

        when(notaRepository.findBySlug(sanitizedSlug)).thenReturn(Optional.of(nota));

        NotaResponseDTO response = readNotaService.execute(rawSlug);

        assertThat(response.slug()).isEqualTo(sanitizedSlug);
        assertThat(response.content()).isEqualTo("Conteúdo da nota");
        verify(notaRepository).findBySlug(sanitizedSlug);
    }

    @Test
    @DisplayName("Deve retornar DTO com conteúdo vazio se a nota não existir")
    void deveRetornarVazioSeNotaNaoExistir() {
        String rawSlug = "nota-inexistente";

        when(notaRepository.findBySlug(rawSlug)).thenReturn(Optional.empty());

        NotaResponseDTO response = readNotaService.execute(rawSlug);

        assertThat(response.slug()).isEqualTo(rawSlug);
        assertThat(response.content()).isEmpty();
        verify(notaRepository).findBySlug(rawSlug);
    }
}
