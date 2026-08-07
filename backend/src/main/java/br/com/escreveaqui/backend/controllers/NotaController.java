package br.com.escreveaqui.backend.controllers;

import br.com.escreveaqui.backend.dtos.NotaRequestDTO;
import br.com.escreveaqui.backend.dtos.NotaResponseDTO;
import br.com.escreveaqui.backend.services.ReadNotaService;
import br.com.escreveaqui.backend.services.UpsertNotaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notes")
@RequiredArgsConstructor
@Validated
public class NotaController {

    private final ReadNotaService readService;
    private final UpsertNotaService upsertService;

    private static final String SLUG_REGEX = "^[A-Za-z0-9_\\s-]+$";

    @GetMapping(value = "/{slug}", produces = "application/json")
    public ResponseEntity<NotaResponseDTO> read(
            @PathVariable @Pattern(regexp = SLUG_REGEX) String slug,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        NotaResponseDTO nota = readService.execute(slug);
        String etag = "W/\"" + nota.updatedAt().toInstant().toEpochMilli() + "\"";

        response.setHeader("ETag", etag);
        response.setHeader("Cache-Control", "no-cache");
        if (etag.equals(request.getHeader("If-None-Match"))) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
        }
        return ResponseEntity.ok().body(nota);
    }

    @PutMapping(value = "/{slug}", consumes = "application/json")
    public ResponseEntity<Void> upsert(
            @PathVariable
            @Pattern(regexp = SLUG_REGEX) String slug,
            @RequestBody
            @Valid NotaRequestDTO request
    ) {
        upsertService.execute(slug, request.content());
        return ResponseEntity.noContent().build();
    }
}