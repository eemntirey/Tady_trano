package mg.emit.tadytrano.infrastructure.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String entete = request.getHeader("Authorization");
        if (entete == null || !entete.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String jeton = entete.substring(7);
        try {
            String email = jwtService.extraireEmail(jeton);
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails utilisateur = userDetailsService.loadUserByUsername(email);
                if (jwtService.estValide(jeton, utilisateur)) {
                    UsernamePasswordAuthenticationToken authentification =
                            new UsernamePasswordAuthenticationToken(utilisateur, null, utilisateur.getAuthorities());
                    authentification.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentification);
                }
            }
        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException e) {
            // Jeton invalide ou compte introuvable : requête non authentifiée
        }

        filterChain.doFilter(request, response);
    }
}
