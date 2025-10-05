package com.a4sync.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final ModProperties modProperties;

    public SecurityConfig(ModProperties modProperties) {
        this.modProperties = modProperties;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        if (!modProperties.isAuthenticationEnabled()) {
            // If authentication is disabled, allow all requests
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable());
        } else {
            // If authentication is enabled, require authentication for all endpoints
            http.authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/v1/**").authenticated()
                    .anyRequest().denyAll())
                .csrf(csrf -> csrf.disable())
                .addFilterBefore(new RepositoryAuthenticationFilter(modProperties), BasicAuthenticationFilter.class);
        }
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
