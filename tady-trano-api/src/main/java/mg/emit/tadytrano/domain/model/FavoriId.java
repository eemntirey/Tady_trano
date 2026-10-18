package mg.emit.tadytrano.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FavoriId implements Serializable {

    private Long utilisateurId;
    private Long annonceId;
}
