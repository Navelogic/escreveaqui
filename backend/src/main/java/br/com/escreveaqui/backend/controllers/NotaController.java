package br.com.escreveaqui.backend.controllers;

import br.com.escreveaqui.backend.dtos.NotaResponseDTO;
import br.com.escreveaqui.backend.services.ReadNotaService;
import br.com.escreveaqui.backend.services.UpsertNotaService;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
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

    @GetMapping("/{slug}")
    public ResponseEntity<NotaResponseDTO> read(
            @PathVariable @Pattern(regexp = SLUG_REGEX) String slug
    ) {
        NotaResponseDTO response = readService.execute(slug);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{slug}")
    public ResponseEntity<Void> upsert(
            @PathVariable @Pattern(regexp = SLUG_REGEX) String slug,
            @RequestBody String content
    ) {
        upsertService.execute(slug, content);
        return ResponseEntity.noContent().build();
    }
}