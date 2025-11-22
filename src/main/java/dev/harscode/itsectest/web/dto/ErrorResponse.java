package dev.harscode.itsectest.web.dto;

public record ErrorResponse(
        String status,
        String code,
        String message,
        Object details
) {
}