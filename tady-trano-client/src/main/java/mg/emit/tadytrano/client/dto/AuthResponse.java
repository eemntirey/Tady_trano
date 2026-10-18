package mg.emit.tadytrano.client.dto;

/** Reponse de connexion / inscription : jeton JWT + profil. */
public record AuthResponse(String token, String type, long expirationEnSecondes, ProfilResponse utilisateur) {
}
