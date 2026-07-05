package com.safar.payment.config;

import com.safar.payment.security.JwtAuthFilter;
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
                .cors(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()
                        // Permit the error dispatch so controller exceptions surface as their
                        // real status (e.g. 500 + message) instead of a misleading 401 from
                        // the re-dispatch hitting anyRequest().authenticated().
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/v1/payments/webhook/**").permitAll()
                        .requestMatchers("/api/v1/payments/tenancy/webhook/**").permitAll()
                        .requestMatchers("/api/v1/payments/fx/currencies").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/donations").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/donations/verify").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/donations/stats").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/donations/leaderboard").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/donations/my").authenticated()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
