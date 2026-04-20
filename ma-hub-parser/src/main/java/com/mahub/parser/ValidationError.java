package com.mahub.parser;

public record ValidationError(
        Severity severity,
        String field,
        String message
) {
    public enum Severity { ERROR, WARNING }
}
