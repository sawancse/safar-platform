package com.safar.flight.config;

import com.safar.flight.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // Public: flight search
                        .requestMatchers(HttpMethod.GET, "/api/v1/flights/search").permitAll()
                        // Public: provider webhooks (HMAC-verified inside controller)
                        .requestMatchers(HttpMethod.POST, "/api/v1/flights/webhooks/**").permitAll()
                        // Admin endpoints
                        .requestMatchers("/api/v1/flights/admin/**").hasRole("ADMIN")
                        // Authenticated: booking, payment, cancel, my bookings
                        .requestMatchers(HttpMethod.POST, "/api/v1/flights/book").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/flights/*/confirm-payment").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/flights/*/cancel").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/flights/my").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/flights/*").authenticated()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
