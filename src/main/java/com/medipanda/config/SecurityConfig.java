package com.medipanda.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwt) throws Exception {
        http.csrf(c->c.disable()).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a->a
                 .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/payments/sslcommerz/**").permitAll()
                .requestMatchers("/api/integrations/config", "/api/integrations/google-login").permitAll()
                .requestMatchers("/api/auth/me", "/api/auth/me/**", "/api/uploads/**").authenticated()
                .requestMatchers("/","/index.html","/pharmacy.html","/css/**","/js/**","/firebase-messaging-sw.js","/images/**","/api/auth/**","/api/pharmacies","/api/pharmacies/**","/api/medicines","/api/medicines/**").permitAll()
                .requestMatchers("/api/orders","/api/customer/**").hasRole("CUSTOMER")
                .requestMatchers("/api/location/**").authenticated()
                .requestMatchers("/api/pharmacy/**").hasRole("PHARMACY")
                .requestMatchers("/api/rider/**").hasRole("RIDER")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/reviews/pharmacy/**").permitAll()
                .requestMatchers("/api/reviews/**","/api/notifications/**","/api/invoices/**","/api/payments/**","/api/reports/**").authenticated()
                .requestMatchers("/api/site").permitAll()
                .anyRequest().authenticated())
            .addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
