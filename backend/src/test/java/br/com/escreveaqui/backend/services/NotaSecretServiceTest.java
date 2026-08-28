package br.com.escreveaqui.backend.services;

import br.com.escreveaqui.backend.dtos.NotaResponseDTO;
import br.com.escreveaqui.backend.exceptions.SegredoInvalidoException;
import br.com.escreveaqui.backend.models.Nota;
import br.com.escreveaqui.backend.repositories.NotaRepository;
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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotaSecretServiceTest {

    @Mock
    private NotaRepository notaRepository;

    private NotaSecretService secretService;

    @BeforeEach
    void setUp() {
        secretService = new NotaSecretService(notaRepository);
    }

    private Nota nota(String slug, String hash) {
        return new Nota(UUID.randomUUID(), slug, "Conteúdo", hash, OffsetDateTime.now(), OffsetDateTime.now());
    }

    @Test
    @DisplayName("Nota sem segredo aceita qualquer entrada")
    void notaSemSegredoAceitaQualquerEntrada() {
        assertThatCode(() -> secretService.check(null, null)).doesNotThrowAnyException();
        assertThatCode(() -> secretService.check(null, "qualquer")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Segredo correto passa, ausente ou errado é rejeitado")
    void validaSegredoContraHash() {
        String hash = secretService.encode("minha-senha-ç");

        assertThatCode(() -> secretService.check(hash, "minha-senha-ç")).doesNotThrowAnyException();
        assertThatThrownBy(() -> secretService.check(hash, "errado")).isInstanceOf(SegredoInvalidoException.class);
        assertThatThrownBy(() -> secretService.check(hash, null)).isInstanceOf(SegredoInvalidoException.class);
    }

    @Test
    @DisplayName("Segredo vazio não vira hash")
    void segredoVazioNaoViraHash() {
        assertThat(secretService.encode(null)).isNull();
        assertThat(secretService.encode("   ")).isNull();
        assertThat(secretService.encode("ok")).isNotNull();
    }

    @Test
    @DisplayName("Leitura de nota protegida exige o segredo correto")
    void leituraDeNotaProtegidaExigeSegredo() {
        String hash = secretService.encode("s3cr3t");
        when(notaRepository.findBySlug("secreta")).thenReturn(Optional.of(nota("secreta", hash)));

        ReadNotaService readService = new ReadNotaService(notaRepository, secretService, new SimpleMeterRegistry());

        assertThatThrownBy(() -> readService.execute("secreta", "errado")).isInstanceOf(SegredoInvalidoException.class);

        NotaResponseDTO ok = readService.execute("secreta", "s3cr3t");
        assertThat(ok.content()).isEqualTo("Conteúdo");
        assertThat(ok.hasSecret()).isTrue();
    }

    @Test
    @DisplayName("Escrita em nota protegida sem segredo correto não chega ao banco")
    void escritaEmNotaProtegidaExigeSegredo() {
        String hash = secretService.encode("s3cr3t");
        when(notaRepository.findBySlug("secreta")).thenReturn(Optional.of(nota("secreta", hash)));

        UpsertNotaService upsertService = new UpsertNotaService(
                notaRepository, new SseService(), secretService, new SimpleMeterRegistry());

        assertThatThrownBy(() -> upsertService.execute("secreta", "novo", null))
                .isInstanceOf(SegredoInvalidoException.class);
        verify(notaRepository, never()).upsert("secreta", "novo", null);
    }

    @Test
    @DisplayName("Segredo é gravado apenas na primeira definição")
    void segredoGravadoApenasNaPrimeiraDefinicao() {
        when(notaRepository.findBySlug("nova")).thenReturn(Optional.empty());

        UpsertNotaService upsertService = new UpsertNotaService(
                notaRepository, new SseService(), secretService, new SimpleMeterRegistry());

        upsertService.execute("nova", "texto", "s3cr3t");

        verify(notaRepository).upsert(org.mockito.ArgumentMatchers.eq("nova"),
                org.mockito.ArgumentMatchers.eq("texto"),
                org.mockito.ArgumentMatchers.argThat(h -> h != null && h.startsWith("$2")));
    }
}
