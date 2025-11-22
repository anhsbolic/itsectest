package dev.harscode.itsectest.security.filter;

import dev.harscode.itsectest.web.exception.UnsupportedMediaTypeException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Component
public class ContentTypeFilter extends OncePerRequestFilter {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/json",
            "multipart/form-data"
    );

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String method = request.getMethod();

        if (HttpMethod.POST.matches(method) || HttpMethod.PUT.matches(method) || HttpMethod.PATCH.matches(method)) {
            String rawContentType = request.getContentType();
            String contentType = rawContentType == null ? "" : rawContentType.split(";")[0].trim();
            if (!contentType.isEmpty() && !ALLOWED_CONTENT_TYPES.contains(contentType)) {
                throw new UnsupportedMediaTypeException("Unsupported content type: " + contentType);
            }
        }

        filterChain.doFilter(request, response);
    }
}
