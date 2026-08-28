package br.com.escreveaqui.backend.controllers;

import br.com.escreveaqui.backend.dtos.NotaResponseDTO;
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

import java.time.OffsetDateTime;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class NotaControllerTest {

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
    @DisplayName("GET /api/v1/notes/{slug} deve retornar 200 OK com DTO e ETag")
    void deveRetornarNotaComETag() throws Exception {
        String slug = "minha-nota";
        OffsetDateTime now = OffsetDateTime.now();
        NotaResponseDTO responseDTO = new NotaResponseDTO(slug, "Texto da nota", false, now);

        when(readService.execute(slug, null)).thenReturn(responseDTO);

        mockMvc.perform(get("/api/v1/notes/{slug}", slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value(slug))
                .andExpect(jsonPath("$.content").value("Texto da nota"))
                .andExpect(header().exists("ETag"))
                .andExpect(header().string("Cache-Control", "no-cache"));

        verify(readService).execute(slug, null);
    }

    @Test
    @DisplayName("GET /api/v1/notes/{slug} deve repassar o slug para o service de leitura")
    void deveRepassarSlugParaService() throws Exception {
        String rawSlug = "Café & Leite";
        NotaResponseDTO responseDTO = new NotaResponseDTO("cafe-leite", "Conteúdo", false, OffsetDateTime.now());

        when(readService.execute(rawSlug, null)).thenReturn(responseDTO);

        mockMvc.perform(get("/api/v1/notes/{slug}", rawSlug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("cafe-leite"));

        verify(readService).execute(rawSlug, null);
    }

    @Test
    @DisplayName("PUT /api/v1/notes/{slug} deve retornar 204 No Content ao salvar")
    void deveSalvarNotaComSucesso() throws Exception {
        String slug = "minha-nota";
        String jsonBody = """
                {
                    "content": "Novo conteúdo da nota"
                }
                """;

        mockMvc.perform(put("/api/v1/notes/{slug}", slug)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isNoContent());

        verify(upsertService).execute(slug, "Novo conteúdo da nota", null);
    }
}
