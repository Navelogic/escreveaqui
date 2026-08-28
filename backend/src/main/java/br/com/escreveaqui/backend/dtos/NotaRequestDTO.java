package br.com.escreveaqui.backend.dtos;

import jakarta.validation.constraints.Size;

public record NotaRequestDTO(
        @Size(max = 1000000, message = "Conteúdo muito extenso para Markdown (limite 1MB)")
        String content,

        @Size(max = 128, message = "Segredo muito longo (limite 128 caracteres)")
        String secret
) {}
