package dev.harscode.itsectest.security.filter;

import dev.harscode.itsectest.security.jwt.JwtTokenService;
import dev.harscode.itsectest.web.exception.UnauthorizedException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.flywaydb.core.internal.util.StringUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (!StringUtils.hasText(header) || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        try {
            JwtTokenService.AccessTokenPayload payload = jwtTokenService.parseAndValidateAccessToken(token);

            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    payload.userId(), null, payload.authorities());
            SecurityContextHolder.getContext().setAuthentication(auth);

            request.setAttribute("auth.userId", payload.userId());
            request.setAttribute("auth.sessionId", payload.sessionId());
            request.setAttribute("auth.role", payload.role());

        } catch (Exception ex) {
            throw new UnauthorizedException("Invalid or expired access token");
        }

        filterChain.doFilter(request, response);
    }
}
