package com.safar.listing.config;

import com.safar.listing.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
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
                        .requestMatchers(HttpMethod.GET, "/api/v1/listings/*/investment-signal").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/listings/*/verification-readiness").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/listings/*/ical/export").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/listings/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/medical-stay/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/experiences/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/marketplace").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/locations/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/sale-properties", "/api/v1/sale-properties/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/sale-properties/admin/reindex").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/builder-projects", "/api/v1/builder-projects/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/builder-projects/admin/reindex").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/experiences/admin/reindex").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/locality-trends", "/api/v1/locality-trends/**").permitAll()
                        .requestMatchers("/api/v1/localities/**").permitAll()
                        .requestMatchers("/api/b2b/v1/**").permitAll()
                        .requestMatchers("/api/v1/internal/**").permitAll()
                        // VAS public endpoints
                        .requestMatchers(HttpMethod.GET, "/api/v1/agreements/templates").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/agreements/stamp-duty/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/homeloan/banks", "/api/v1/homeloan/banks/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/homeloan/emi/calculate").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/legal/packages").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/legal/advocates").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/legal/cases/*/report", "/api/v1/legal/cases/*/report.pdf").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/interiors/designers", "/api/v1/interiors/designers/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/interiors/materials/catalog").permitAll()
                        .requestMatchers("/api/v1/agreements/admin/**").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/agreements/*/status").authenticated()
                        .requestMatchers("/api/v1/interiors/admin/**").authenticated()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

}
