package com.safar.services.config;

import com.safar.services.security.JwtAuthFilter;
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
                        // Internal service-to-service (admin merge, etc.) — restricted to VPC in prod
                        .requestMatchers("/api/v1/internal/**").permitAll()
                        // Admin: dish catalog CRUD (must be before public GET dishes rule)
                        .requestMatchers("/api/v1/dishes/admin/**").authenticated()
                        // Public: dish catalog & cook matching
                        .requestMatchers(HttpMethod.GET, "/api/v1/dishes", "/api/v1/dishes/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/dishes/match-chefs").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/chefs/*/dish-offerings").permitAll()
                        .requestMatchers("/api/v1/chefs/admin/**").authenticated()
                        .requestMatchers("/api/v1/chefs/me").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/v1/chefs/me/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/v1/chefs/me/availability").authenticated()
                        // Chef-owned staff roster — auth required on ALL methods (overrides the public GET rule below)
                        .requestMatchers("/api/v1/chefs/me/staff", "/api/v1/chefs/me/staff/**").authenticated()
                        // Platform staff pool — admin only
                        .requestMatchers("/api/v1/staff/admin/**").authenticated()
                        .requestMatchers("/api/v1/chef-bookings/admin/**").authenticated()
                        .requestMatchers("/api/v1/chef-events/admin/**").authenticated()
                        .requestMatchers("/api/v1/chef-subscriptions/admin/**").authenticated()
                        .requestMatchers("/api/v1/chef-events/pricing/admin/**").authenticated()
                        .requestMatchers("/api/v1/chef-bookings/my", "/api/v1/chef-bookings/chef").authenticated()
                        .requestMatchers("/api/v1/chef-subscriptions/my", "/api/v1/chef-subscriptions/chef").authenticated()
                        .requestMatchers("/api/v1/chef-events/my", "/api/v1/chef-events/chef").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/chef-events/pricing").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/chef-events/aggregate-ratings").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/chefs", "/api/v1/chefs/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/chefs/menus/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/chef-events").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/chef-events", "/api/v1/chef-events/{id}").permitAll()
                        // Public: chef photos, calendar, cuisine pricing (read), tracking, invoices
                        .requestMatchers(HttpMethod.GET, "/api/v1/chefs/photos/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/chefs/availability/*/calendar").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/chefs/cuisine-pricing/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/chef-bookings/*/tracking").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/chef-bookings/*/invoice").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/chef-events/*/invoice").permitAll()
                        // Services-leg listings: admin queue + lifecycle (admin)
                        .requestMatchers("/api/v1/services/admin/**").authenticated()
                        // Services-leg listings: vendor-owned ops
                        .requestMatchers("/api/v1/services/listings/me", "/api/v1/services/listings/me/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/services/listings").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/services/listings/*").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/services/listings/*/submit").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/services/listings/*/pause").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/services/listings/*/resume").authenticated()
                        .requestMatchers("/api/v1/services/listings/*/kyc-documents", "/api/v1/services/listings/*/kyc-documents/**").authenticated()
                        // Service items: vendor-owned writes, public reads
                        .requestMatchers(HttpMethod.GET, "/api/v1/services/listings/*/items").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/services/items/*").permitAll()
                        .requestMatchers("/api/v1/services/listings/*/items/all").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/services/listings/*/items").authenticated()
                        .requestMatchers("/api/v1/services/items/**").authenticated()
                        // Vendor invites — token resolve is public (the token IS the auth)
                        .requestMatchers(HttpMethod.GET, "/api/v1/services/invites/**").permitAll()
                        // Services-leg listings: public storefront browse
                        .requestMatchers(HttpMethod.GET, "/api/v1/services/listings", "/api/v1/services/listings/by-slug/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/services/listings/*").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
