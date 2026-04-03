package com.safar.chef.config;

import com.safar.chef.security.JwtAuthFilter;
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
                        .requestMatchers("/api/v1/chefs/admin/**").authenticated()
                        .requestMatchers("/api/v1/chef-events/pricing/admin/**").authenticated()
                        .requestMatchers("/api/v1/chef-events/my", "/api/v1/chef-events/chef").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/chef-events/pricing").permitAll()
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
                        .anyRequest().authenticated()
                )
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
