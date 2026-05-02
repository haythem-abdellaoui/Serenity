package tn.esprit.arctic.derbelmicroservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final DerbelJwtValidator jwtValidator;
    private final UserServiceIdentityClient userServiceIdentityClient;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String bearerHeader = request.getHeader("Authorization");
        String jwt = extractJwt(request);
        if (!StringUtils.hasText(jwt)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = jwtValidator.parseAndValidate(jwt);
            Long userId = extractUserId(claims, bearerHeader);
            List<GrantedAuthority> authorities = parseAuthorities(claims.get("roles", String.class));

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (JwtException | IllegalArgumentException e) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private Long extractUserId(Claims claims, String bearerHeader) {
        Object value = claims.get("userId");
        if (value != null) {
            if (value instanceof Number number) {
                return number.longValue();
            }
            return Long.parseLong(value.toString());
        }
        if (!StringUtils.hasText(bearerHeader)) {
            throw new JwtException("JWT missing userId claim");
        }
        return userServiceIdentityClient.resolveUserIdFromBearer(bearerHeader);
    }

    private static List<GrantedAuthority> parseAuthorities(String roles) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (!StringUtils.hasText(roles)) {
            return authorities;
        }
        for (String role : roles.split(",")) {
            String trimmed = role.trim();
            if (!trimmed.isEmpty()) {
                authorities.add(new SimpleGrantedAuthority(trimmed));
            }
        }
        return authorities;
    }

    private static String extractJwt(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
