package br.com.escreveaqui.backend.controllers;

import br.com.escreveaqui.backend.services.DeleteNotaService;
import br.com.escreveaqui.backend.services.ReadNotaService;
import br.com.escreveaqui.backend.services.UpsertNotaService;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Validated
public class NotaController {

    private final ReadNotaService readService;
    private final UpsertNotaService upsertService;
    private final DeleteNotaService deleteService;

    @GetMapping(value = "/{slug}", produces = "text/plain")
    public String read(
            @PathVariable
            @Pattern(regexp = "^[A-Za-z0-9_-]+$")
            String slug
    ) {
        return readService.execute(slug);
    }

    @PutMapping(value = "/{slug}", consumes = "text/plain")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void upsert(
            @PathVariable
            @Pattern(regexp = "^[A-Za-z0-9_-]+$")
            String slug,
            @RequestBody String content
    ) {
        upsertService.execute(slug, content);
    }

    @DeleteMapping("/{slug}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable
            @Pattern(regexp = "^[A-Za-z0-9_-]+$")
            String slug
    ) {
        deleteService.execute(slug);
    }
}

