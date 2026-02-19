package br.com.escreveaqui.backend.dtos;

import java.time.OffsetDateTime;

public record NotaResponseDTO(
        String slug,
        String content,
        OffsetDateTime updatedAt
) {}