package mg.emit.tadytrano.application.dto;

public record AuthResponse(
        String token,
        String type,
        long expirationEnSecondes,
        ProfilResponse utilisateur
) {
}
