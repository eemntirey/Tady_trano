package mg.emit.tadytrano.client.dto;

/** Coordonnees du proprietaire, exposees uniquement aux connectes. */
public record ContactProprietaireResponse(String nom, String telephone, String emailContact) {
}
