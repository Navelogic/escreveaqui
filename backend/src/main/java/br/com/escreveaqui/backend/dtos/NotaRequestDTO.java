package br.com.escreveaqui.backend.dtos;

import jakarta.validation.constraints.Size;

public record NotaRequestDTO(
        @Size(max = 1000000, message = "Conteúdo muito extenso para Markdown (limite 1MB)")
        String content
) {}
