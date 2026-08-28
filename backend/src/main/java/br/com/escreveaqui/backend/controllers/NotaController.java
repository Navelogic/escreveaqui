package br.com.escreveaqui.backend.controllers;

import br.com.escreveaqui.backend.dtos.NotaRequestDTO;
import br.com.escreveaqui.backend.dtos.NotaResponseDTO;
import br.com.escreveaqui.backend.exceptions.SegredoInvalidoException;
import br.com.escreveaqui.backend.services.NotaSecretService;
import br.com.escreveaqui.backend.services.ReadNotaService;
import br.com.escreveaqui.backend.services.SseService;
import br.com.escreveaqui.backend.services.UpsertNotaService;
import br.com.escreveaqui.backend.utils.SlugUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@RestController
@RequestMapping("/api/v1/notes")
@RequiredArgsConstructor
@Validated
public class NotaController {

    public static final String SECRET_HEADER = "X-Nota-Secret";

    private final ReadNotaService readService;
    private final UpsertNotaService upsertService;
    private final NotaSecretService secretService;
    private final SseService sseService;

    @GetMapping(value = "/{slug}", produces = "application/json")
    public ResponseEntity<NotaResponseDTO> read(
            @PathVariable @Pattern(regexp = SlugUtils.SLUG_REGEX) String slug,
            @RequestHeader(value = SECRET_HEADER, required = false) String secret,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        NotaResponseDTO nota = readService.execute(slug, decodeSecret(secret));
        String etag = "W/\"" + nota.updatedAt().toInstant().toEpochMilli() + "\"";

        response.setHeader("ETag", etag);
        response.setHeader("Cache-Control", "no-cache");
        if (etag.equals(request.getHeader("If-None-Match"))) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
        }
        return ResponseEntity.ok().body(nota);
    }

    @GetMapping(value = "/{slug}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
            @PathVariable @Pattern(regexp = SlugUtils.SLUG_REGEX) String slug,
            @RequestParam(required = false) String secret,
            HttpServletResponse response
    ) throws IOException {
        try {
            secretService.verify(slug, decodeSecret(secret));
        } catch (SegredoInvalidoException e) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Nota protegida");
            return null;
        }
        return sseService.subscribe(slug);
    }

    @PutMapping(value = "/{slug}", consumes = "application/json")
    public ResponseEntity<Void> upsert(
            @PathVariable
            @Pattern(regexp = SlugUtils.SLUG_REGEX) String slug,
            @RequestBody
            @Valid NotaRequestDTO request
    ) {
        upsertService.execute(slug, request.content(), request.secret());
        return ResponseEntity.noContent().build();
    }

    private static String decodeSecret(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return null;
        }
        try {
            return new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
