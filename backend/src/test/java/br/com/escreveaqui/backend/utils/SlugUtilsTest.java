package br.com.escreveaqui.backend.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class SlugUtilsTest {

    @ParameterizedTest
    @CsvSource({
            "'Minha Nota', 'minha-nota'",
            "'Olá mundo!', 'ola-mundo'",
            "'abc_123', 'abc123'",
            "')*())}__---', ''",
            "'  teste   nota  ', 'teste-nota'",
            "'---slug---', 'slug'",
            "'Açúcar e Café', 'acucar-e-cafe'"
    })
    @DisplayName("Deve formatar o slug corretamente removendo acentos e caracteres especiais")
    void deveFormatSlugCorretamente(String input, String expected) {
        String result = SlugUtils.format(input);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("Deve retornar string vazia para entrada nula")
    void deveRetornarVazioParaNull() {
        assertThat(SlugUtils.format(null)).isEmpty();
    }
}
