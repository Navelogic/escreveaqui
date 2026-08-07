package br.com.escreveaqui.backend.models;

import java.time.OffsetDateTime;
import java.util.UUID;

public record Nota(
        UUID id,
        String slug,
        String content,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
