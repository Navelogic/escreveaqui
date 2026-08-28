package br.com.escreveaqui.backend.controllers;

import br.com.escreveaqui.backend.services.NotaSecretService;
import br.com.escreveaqui.backend.services.ReadNotaService;
import br.com.escreveaqui.backend.services.SseService;
import br.com.escreveaqui.backend.services.UpsertNotaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class NotaControllerSseTest {

    private MockMvc mockMvc;

    @Mock
    private ReadNotaService readService;

    @Mock
    private UpsertNotaService upsertService;

    @Mock
    private NotaSecretService secretService;

    @Mock
    private SseService sseService;

    @InjectMocks
    private NotaController notaController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(notaController).build();
    }

    @Test
    @DisplayName("GET /api/v1/notes/{slug}/stream deve iniciar transmissão SSE assíncrona")
    void deveIniciarTransmissaoSse() throws Exception {
        String slug = "minha-nota";
        SseEmitter emitter = new SseEmitter(0L);

        when(sseService.subscribe(slug)).thenReturn(emitter);

        mockMvc.perform(get("/api/v1/notes/{slug}/stream", slug)
                        .accept(MediaType.TEXT_EVENT_STREAM_VALUE))
                .andExpect(request().asyncStarted());

        verify(sseService).subscribe(slug);
    }
}
