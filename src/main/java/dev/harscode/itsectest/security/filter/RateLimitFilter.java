package dev.harscode.itsectest.security.filter;

import dev.harscode.itsectest.web.exception.TooManyRequestsException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    // 100 request / minutes for each IP address
    private static final int MAX_REQUESTS_PER_MINUTE = 100;

    private final Map<String, Window> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String clientIp = request.getRemoteAddr();
        String key = "global:" + clientIp;

        if (!allowRequest(key)) {
            throw new TooManyRequestsException("Too many requests, please try again later.");
        }

        filterChain.doFilter(request, response);
    }

    private boolean allowRequest(String key) {
        long now = Instant.now().getEpochSecond();
        long currentWindow = now / 60;

        Window window = buckets.computeIfAbsent(key, k -> new Window(currentWindow, 0));

        synchronized (window) {
            if (window.window != currentWindow) {
                window.window = currentWindow;
                window.count = 0;
            }
            window.count++;
            return window.count <= MAX_REQUESTS_PER_MINUTE;
        }
    }

    private static class Window {
        long window;
        int count;

        Window(long window, int count) {
            this.window = window;
            this.count = count;
        }
    }
}
