package br.com.escreveaqui.backend.exceptions;

public class SegredoInvalidoException extends RuntimeException {

    public SegredoInvalidoException() {
        super("Segredo ausente ou incorreto para esta nota.");
    }
}
