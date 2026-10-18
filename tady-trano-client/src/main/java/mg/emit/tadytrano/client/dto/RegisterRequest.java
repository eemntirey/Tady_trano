package mg.emit.tadytrano.client.dto;

/** Inscription d'un nouveau compte. */
public record RegisterRequest(String email, String motDePasse, String nom, String prenom, String role) {
}
