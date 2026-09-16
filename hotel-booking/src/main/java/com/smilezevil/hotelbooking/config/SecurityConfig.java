package com.smilezevil.hotelbooking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/hotels/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/rooms/**").permitAll()

                        .requestMatchers(HttpMethod.POST, "/api/**").hasAnyRole("ADMIN", "admin")
                        .requestMatchers(HttpMethod.PUT, "/api/**").hasAnyRole("ADMIN", "admin")
                        .requestMatchers(HttpMethod.DELETE, "/api/**").hasAnyRole("ADMIN", "admin")

                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userAuthoritiesMapper(userAuthoritiesMapper())
                        )
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                );

        return http.build();
    }

    @Bean
    public GrantedAuthoritiesMapper userAuthoritiesMapper() {
        return authorities -> {
            Set<GrantedAuthority> mappedAuthorities = new HashSet<>();

            authorities.forEach(authority -> {
                mappedAuthorities.add(authority);

                Map<String, Object> attributes = null;
                if (authority instanceof OidcUserAuthority oidcAuth) {
                    attributes = oidcAuth.getIdToken().getClaims();
                } else if (authority instanceof OAuth2UserAuthority oauth2Auth) {
                    attributes = oauth2Auth.getAttributes();
                }

                if (attributes != null && attributes.containsKey("realm_access")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> realmAccess = (Map<String, Object>) attributes.get("realm_access");
                    if (realmAccess != null && realmAccess.containsKey("roles")) {
                        @SuppressWarnings("unchecked")
                        List<String> roles = (List<String>) realmAccess.get("roles");
                        if (roles != null) {
                            roles.forEach(role -> mappedAuthorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
                        }
                    }
                }
            });

            return mappedAuthorities;
        };
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            if (realmAccess == null || !realmAccess.containsKey("roles")) {
                return List.of();
            }
            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) realmAccess.get("roles");
            return roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toList());
        });
        return converter;
    }
}