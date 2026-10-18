package mg.emit.tadytrano.client.dto;

/** Ajout d'une photo a une annonce par son URL. */
public record PhotoRequest(String url, boolean principale) {
}
