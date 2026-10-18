-- Tady Trano - schema initial (CDC section 10)

CREATE TABLE ville (
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    nom  VARCHAR(100) NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quartier (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    nom       VARCHAR(100) NOT NULL,
    ville_id  BIGINT NOT NULL,
    latitude  DOUBLE NULL,
    longitude DOUBLE NULL,
    CONSTRAINT fk_quartier_ville FOREIGN KEY (ville_id) REFERENCES ville (id),
    CONSTRAINT uq_quartier_nom_ville UNIQUE (nom, ville_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE type_location (
    id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    code    VARCHAR(20) NOT NULL UNIQUE,
    libelle VARCHAR(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE utilisateur (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    email            VARCHAR(190) NOT NULL UNIQUE,
    mot_de_passe     VARCHAR(100) NOT NULL,
    nom              VARCHAR(50)  NOT NULL,
    prenom           VARCHAR(50)  NOT NULL,
    role             VARCHAR(20)  NOT NULL CHECK (role IN ('LOCATAIRE', 'PROPRIETAIRE', 'ADMIN')),
    actif            BOOLEAN      NOT NULL DEFAULT TRUE,
    date_inscription DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE proprietaire (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    utilisateur_id BIGINT       NOT NULL UNIQUE,
    type           VARCHAR(20)  NOT NULL CHECK (type IN ('PARTICULIER', 'AGENCE')),
    nom_agence     VARCHAR(120) NULL,
    telephone      VARCHAR(30)  NOT NULL,
    email_contact  VARCHAR(190) NULL,
    quartier_id    BIGINT       NULL,
    CONSTRAINT fk_proprietaire_utilisateur FOREIGN KEY (utilisateur_id) REFERENCES utilisateur (id),
    CONSTRAINT fk_proprietaire_quartier FOREIGN KEY (quartier_id) REFERENCES quartier (id),
    CONSTRAINT ck_proprietaire_agence CHECK ((type = 'AGENCE' AND nom_agence IS NOT NULL) OR (type = 'PARTICULIER' AND nom_agence IS NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE annonce (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    proprietaire_id  BIGINT         NOT NULL,
    type_location_id BIGINT         NOT NULL,
    quartier_id      BIGINT         NOT NULL,
    titre            VARCHAR(150)   NOT NULL,
    description      VARCHAR(2000) NULL,
    prix             DECIMAL(12, 2) NOT NULL,
    surface          INT            NULL,
    nb_pieces        INT            NOT NULL,
    latitude         DOUBLE         NULL,
    longitude        DOUBLE         NULL,
    statut           VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE' CHECK (statut IN ('ACTIVE', 'LOUEE', 'MASQUEE')),
    date_publication DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_annonce_proprietaire FOREIGN KEY (proprietaire_id) REFERENCES proprietaire (id),
    CONSTRAINT fk_annonce_type FOREIGN KEY (type_location_id) REFERENCES type_location (id),
    CONSTRAINT fk_annonce_quartier FOREIGN KEY (quartier_id) REFERENCES quartier (id),
    CONSTRAINT ck_annonce_prix CHECK (prix >= 0),
    CONSTRAINT ck_annonce_pieces CHECK (nb_pieces >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE photo (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    annonce_id BIGINT       NOT NULL,
    url        VARCHAR(500) NOT NULL,
    principale BOOLEAN      NOT NULL DEFAULT FALSE,
    ordre      INT          NOT NULL DEFAULT 0,
    CONSTRAINT fk_photo_annonce FOREIGN KEY (annonce_id) REFERENCES annonce (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE favori (
    utilisateur_id BIGINT   NOT NULL,
    annonce_id     BIGINT   NOT NULL,
    date_ajout     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (utilisateur_id, annonce_id),
    CONSTRAINT fk_favori_utilisateur FOREIGN KEY (utilisateur_id) REFERENCES utilisateur (id) ON DELETE CASCADE,
    CONSTRAINT fk_favori_annonce FOREIGN KEY (annonce_id) REFERENCES annonce (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Index (CDC 8.1 - performance de recherche)
CREATE INDEX idx_annonce_filtres ON annonce (statut, quartier_id, prix, nb_pieces);
CREATE INDEX idx_annonce_geo ON annonce (latitude, longitude);
CREATE INDEX idx_annonce_date ON annonce (date_publication);
CREATE INDEX idx_annonce_type ON annonce (type_location_id);
CREATE INDEX idx_photo_annonce ON photo (annonce_id);

-- Types de location (CDC section 3)
INSERT INTO type_location (code, libelle) VALUES
    ('HOTEL', 'Hôtel'),
    ('ETAGE', 'Étage'),
    ('PIECE', 'Par pièce');
