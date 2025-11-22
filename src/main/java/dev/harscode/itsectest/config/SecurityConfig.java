package dev.harscode.itsectest.config;

import dev.harscode.itsectest.security.filter.ContentTypeFilter;
import dev.harscode.itsectest.security.filter.FingerprintFilter;
import dev.harscode.itsectest.security.filter.JwtAuthenticationFilter;
import dev.harscode.itsectest.security.filter.RateLimitFilter;
import dev.harscode.itsectest.security.handler.RestAccessDeniedHandler;
import dev.harscode.itsectest.security.handler.RestAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler,
            RateLimitFilter rateLimitFilter,
            ContentTypeFilter contentTypeFilter,
            FingerprintFilter fingerprintFilter,
            JwtAuthenticationFilter jwtAuthenticationFilter
    ) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/auth/register",
                                "/auth/email-verification",
                                "/auth/login",
                                "/auth/refresh",
                                "/auth/forgot-password",
                                "/auth/reset-password"
                        ).permitAll()
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        .requestMatchers("/bubble-sort/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .anyRequest()
                        .authenticated()
                );

        http.addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(contentTypeFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(fingerprintFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
