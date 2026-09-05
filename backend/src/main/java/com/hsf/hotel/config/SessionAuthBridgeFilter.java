package com.hsf.hotel.config;

import com.hsf.hotel.user.model.User;
import com.hsf.hotel.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Bridges the legacy session-attribute auth model with Spring Security so
 * SecurityFilterChain rules (hasRole, etc.) work for endpoints that already
 * check the session manually. Ordered to run after {@code RequestIdFilter},
 * {@code SecurityHeadersFilter}, and {@code CsrfCookieFilter} so the
 * security headers, correlation id, and CSRF check are all in place by
 * the time Spring Security applies its own authorisation rules.
 *
 * The bridge explicitly saves the SecurityContext to the HTTP session
 * (under the standard {@code SPRING_SECURITY_CONTEXT} attribute) so that
 * Spring Security's downstream {@code SecurityContextHolderFilter} can
 * pick it up. Without that explicit save, the downstream filter would
 * load an empty SecurityContext from the repository and clear the
 * ThreadLocal context that this filter just populated.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 30)
public class SessionAuthBridgeFilter extends OncePerRequestFilter {

    private static final String SPRING_SECURITY_CONTEXT_KEY =
            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;

    private final SecurityContextHolderStrategy holderStrategy =
            SecurityContextHolder.getContextHolderStrategy();
    private final SecurityContextRepository contextRepository =
            new HttpSessionSecurityContextRepository();
    private final UserRepository userRepository;

    public SessionAuthBridgeFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object u = session.getAttribute("user");
            if (u instanceof User user) {
                User current = user.getId() != null
                        ? userRepository.findById(user.getId()).orElse(null)
                        : userRepository.findByUsername(user.getUsername()).orElse(null);
                if (current == null || Boolean.TRUE.equals(current.getDisabled())) {
                    session.invalidate();
                    // Let the request proceed as an unauthenticated guest.
                    // If the endpoint requires authentication, Spring Security will return 401.
                } else {
                session.setAttribute("user", current);
                List<SimpleGrantedAuthority> authorities = "ADMIN".equals(current.getRole())
                        ? List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_USER"))
                        : List.of(new SimpleGrantedAuthority("ROLE_USER"));
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(current.getUsername(), null, authorities);
                SecurityContext context = holderStrategy.createEmptyContext();
                context.setAuthentication(auth);
                holderStrategy.setContext(context);
                // Persist to session so Spring Security's SecurityContextHolderFilter
                // sees the same principal when it loads from the repository.
                session.setAttribute(SPRING_SECURITY_CONTEXT_KEY, context);
                }
            }
        }
        try {
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
