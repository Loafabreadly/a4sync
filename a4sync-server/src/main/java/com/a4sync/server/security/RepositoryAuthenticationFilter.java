package com.a4sync.server.security;

import com.a4sync.server.config.ModProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.lang.NonNull;

import java.io.IOException;
import java.util.Collections;

public class RepositoryAuthenticationFilter extends OncePerRequestFilter {
    private final ModProperties modProperties;

    public RepositoryAuthenticationFilter(ModProperties modProperties) {
        this.modProperties = modProperties;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        
        String authHeader = request.getHeader("X-Repository-Auth");
        
        if (authHeader != null) {
            // The client should send the SHA-256 hash of the password
            if (BCrypt.checkpw(authHeader, modProperties.getRepositoryPassword())) {
                var authentication = new UsernamePasswordAuthenticationToken(
                    "repository-user",
                    null,
                    Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"))
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
