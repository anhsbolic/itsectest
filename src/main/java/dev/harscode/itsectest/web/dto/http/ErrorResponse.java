package dev.harscode.itsectest.web.dto.http;

public record ErrorResponse(
        String status,
        String code,
        String message,
        Object details
) {
}