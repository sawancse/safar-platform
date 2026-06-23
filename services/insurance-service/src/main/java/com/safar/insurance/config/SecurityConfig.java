package com.safar.insurance.config;

import com.safar.insurance.security.JwtAuthFilter;
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
                        // Public quote (no login needed to see premiums)
                        .requestMatchers(HttpMethod.GET, "/api/v1/insurance/quote").permitAll()
                        // Standalone marketplace: catalog + quote public, buy authenticated (anyRequest)
                        .requestMatchers(HttpMethod.GET, "/api/v1/insurance/marketplace/products").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/insurance/marketplace/quote").permitAll()
                        // PolicyBazaar-style compare + advisor callback — public (no login to compare or request a call)
                        .requestMatchers(HttpMethod.POST, "/api/v1/insurance/marketplace/compare").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/insurance/marketplace/advisor-callback").permitAll()
                        // Certificate of insurance — policyRef-gated public link (the cert email points here)
                        .requestMatchers(HttpMethod.GET, "/api/v1/insurance/certificate/**").permitAll()
                        // Policy-wording document — public read-only
                        .requestMatchers(HttpMethod.GET, "/api/v1/insurance/policy-wording").permitAll()
                        // Server-to-server issuance — no JWT (booking-service direct call); secret-gated in the controller
                        .requestMatchers(HttpMethod.POST, "/api/v1/insurance/marketplace/internal/issue").permitAll()
                        // Razorpay webhook — HMAC-verified in the controller, no user JWT
                        .requestMatchers(HttpMethod.POST, "/api/v1/insurance/marketplace/webhook/razorpay").permitAll()
                        // Admin endpoints
                        .requestMatchers("/api/v1/insurance/admin/**").hasRole("ADMIN")
                        // Authenticated booking + management
                        .requestMatchers(HttpMethod.POST, "/api/v1/insurance/issue").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/insurance/*/cancel").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/insurance/my").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/insurance/*").authenticated()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
