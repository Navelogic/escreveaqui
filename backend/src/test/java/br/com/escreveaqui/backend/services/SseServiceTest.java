package br.com.escreveaqui.backend.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class SseServiceTest {

    private SseService sseService;

    @BeforeEach
    void setUp() {
        sseService = new SseService();
    }

    @Test
    @DisplayName("Deve criar e retornar um SseEmitter para o slug informado")
    void deveInscreverSlugComSucesso() {
        String rawSlug = "Minha Nota SSE!";
        SseEmitter emitter = sseService.subscribe(rawSlug);

        assertThat(emitter).isNotNull();
    }

    @Test
    @DisplayName("Deve notificar sem exceção quando não houver inscritos")
    void deveNotificarSemInscritosSemLancarExcecao() {
        assertThatCode(() -> sseService.notify("slug-sem-inscritos", "Novo Conteudo"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Deve notificar inscritos sanitizando o slug")
    void deveNotificarInscritosSanitizandoSlug() {
        String rawSlug = "Nota Compartilhada";
        SseEmitter emitter = sseService.subscribe(rawSlug);

        assertThatCode(() -> sseService.notify("nota-compartilhada", "Conteudo Atualizado"))
                .doesNotThrowAnyException();
    }
}
