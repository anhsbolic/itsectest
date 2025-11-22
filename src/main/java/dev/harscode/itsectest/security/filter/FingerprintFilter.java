package dev.harscode.itsectest.security.filter;

import dev.harscode.itsectest.security.context.RequestContext;
import dev.harscode.itsectest.security.context.RequestFingerprint;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
public class FingerprintFilter extends OncePerRequestFilter {

    private final RequestContext requestContext;

    public FingerprintFilter(RequestContext requestContext) {
        this.requestContext = requestContext;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            String ip = extractClientIp(request);
            String ua = Optional.ofNullable(request.getHeader(HttpHeaders.USER_AGENT)).orElse("unknown");

            RequestFingerprint fingerprint = new RequestFingerprint(ip, ua);
            requestContext.set(fingerprint);

            filterChain.doFilter(request, response);
        } finally {
            requestContext.clear();
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            // get the first IP address from the list
            return xff.split(",")[0].trim();
        }
        String xri = request.getHeader("X-Real-IP");
        if (xri != null && !xri.isBlank()) {
            return xri;
        }
        return request.getRemoteAddr();
    }
}