package com.ste.restaurant.configuration;

import com.ste.restaurant.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import com.ste.restaurant.service.CustomUserDetailsService;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(CustomUserDetailsService customUserDetailsService,
                          JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.customUserDetailsService = customUserDetailsService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.csrf().disable()
                .authorizeHttpRequests(auth -> auth // "/rest/api/orders/**"
                        .requestMatchers("/actuator/**").permitAll()  // delete in prod
                        .requestMatchers(
                                "/rest/api/auth/**",         // Authentication endpoints (login, register, etc.)
                                "/v3/api-docs/**",           // Swagger/OpenAPI docs
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/images/**",
                                "/qr-codes/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/rest/api/tables/available").permitAll()  // Public: get available tables
                        .requestMatchers(HttpMethod.GET, "/rest/api/menus/active").permitAll()      // Public: get active menus
                        .requestMatchers(HttpMethod.GET, "/rest/api/categories").permitAll()       // Public get all categories
                        .requestMatchers(HttpMethod.GET, "/rest/api/food-items/*/categories").permitAll()
                        .requestMatchers(HttpMethod.GET, "/rest/api/users/*/addresses/**").hasRole("ADMIN")
                        // .requestMatchers("/qr-codes/**").hasRole("ADMIN")

                        // --- DEFAULT RULE ---
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .userDetailsService(customUserDetailsService)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(customUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
