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
        
        // Only enforce authentication if it's enabled
        if (!modProperties.isAuthenticationEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }
        
        String authHeader = request.getHeader("X-Repository-Auth");
        
        if (authHeader != null) {
            // The client sends the plain text password which we verify against BCrypt hash
            if (modProperties.verifyPassword(authHeader)) {
                var authentication = new UsernamePasswordAuthenticationToken(
                    "repository-user",
                    null,
                    Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"))
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Invalid repository password");
                return;
            }
        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Repository password required");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
