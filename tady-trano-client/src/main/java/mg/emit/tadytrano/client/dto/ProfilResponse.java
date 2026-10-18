package mg.emit.tadytrano.client.dto;

/** Profil de l'utilisateur connecte. */
public record ProfilResponse(Long id, String email, String nom, String prenom, String role) {
}
