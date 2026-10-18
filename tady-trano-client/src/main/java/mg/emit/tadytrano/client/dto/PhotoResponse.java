package mg.emit.tadytrano.client.dto;

/** Photo d'une annonce (URL distante). */
public record PhotoResponse(Long id, String url, boolean principale, int ordre) {
}
