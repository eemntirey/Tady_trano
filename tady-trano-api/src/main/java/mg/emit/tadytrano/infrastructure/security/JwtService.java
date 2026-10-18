package mg.emit.tadytrano.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    public String genererToken(UserDetails utilisateur) {
        Date maintenant = new Date();
        return Jwts.builder()
                .subject(utilisateur.getUsername())
                .issuedAt(maintenant)
                .expiration(new Date(maintenant.getTime() + expirationMs))
                .signWith(clePrivee(), Jwts.SIG.HS256)
                .compact();
    }

    public String extraireEmail(String token) {
        return extraireClaims(token).getSubject();
    }

    public boolean estValide(String token, UserDetails utilisateur) {
        try {
            Claims claims = extraireClaims(token);
            return claims.getSubject().equals(utilisateur.getUsername())
                    && claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public long dureeDeVieEnSecondes() {
        return expirationMs / 1000;
    }

    private Claims extraireClaims(String token) {
        return Jwts.parser()
                .verifyWith(clePrivee())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey clePrivee() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
