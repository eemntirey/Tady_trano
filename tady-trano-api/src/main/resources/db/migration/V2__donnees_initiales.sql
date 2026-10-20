-- Tady Trano - Donnees initiales et de demonstration (CDC sections 2, 3, 4, 10)
-- Mot de passe pour tous les utilisateurs : Pass1234!
-- Hash BCrypt (cout 12) : $2a$12$dhyfvTpmj.H.GLzfIj9U5uorcu8entf8AlD7AM.Fre/6FNYrS9Dpe

-- 1. Villes
INSERT INTO ville (id, nom) VALUES
    (1, 'Fianarantsoa'),
    (2, 'Antananarivo');

-- 2. Quartiers avec coordonnees GPS
-- Fianarantsoa (centre: -21.4536, 47.0858)
INSERT INTO quartier (id, nom, ville_id, latitude, longitude) VALUES
    (1, 'Andrainjato (Campus EMIT)', 1, -21.4677, 47.1092),
    (2, 'Ampasambazaha (Centre)', 1, -21.4528, 47.0863),
    (3, 'Ambalapaiso', 1, -21.4485, 47.0821),
    (4, 'Tanambao', 1, -21.4420, 47.0790),
    (5, 'Isaha', 1, -21.4600, 47.0910),
    (11, 'Soarano', 1, -21.4550, 47.0880),
    (12, 'Ambatovinaky', 1, -21.4500, 47.0850),
    (13, 'Mahazoarivo', 1, -21.4650, 47.0920),
    (14, 'Ankidodo', 1, -21.4480, 47.0800),
    (15, 'Tanana Ambany', 1, -21.4400, 47.0780);

-- Antananarivo (centre: -18.8792, 47.5079)
INSERT INTO quartier (id, nom, ville_id, latitude, longitude) VALUES
    (6, 'Ankatso (Campus Univ)', 2, -18.9175, 47.5562),
    (7, 'Analakely (Centre)', 2, -18.9056, 47.5255),
    (8, 'Ivandry (Résidentiel)', 2, -18.8752, 47.5230),
    (9, 'Itaosy', 2, -18.9320, 47.4720),
    (10, 'Talatamaty (Aéroport)', 2, -18.8350, 47.4680),
    (16, 'Ambohijatovo', 2, -18.8900, 47.5100),
    (17, 'Isotry', 2, -18.8850, 47.5050),
    (18, 'Ambondrona', 2, -18.8700, 47.5200),
    (19, 'Amparibe', 2, -18.9100, 47.5400),
    (20, 'Antsakaviro', 2, -18.8950, 47.5000);

-- 3. Utilisateurs de demonstration
INSERT INTO utilisateur (id, email, mot_de_passe, nom, prenom, role, actif, date_inscription) VALUES
    (1, 'admin@tady.mg', '$2a$12$dhyfvTpmj.H.GLzfIj9U5uorcu8entf8AlD7AM.Fre/6FNYrS9Dpe', 'Admin', 'Jean', 'ADMIN', TRUE, '2026-10-01 08:00:00'),
    (2, 'rakoto.proprio@gmail.com', '$2a$12$dhyfvTpmj.H.GLzfIj9U5uorcu8entf8AlD7AM.Fre/6FNYrS9Dpe', 'Rakoto', 'Hery', 'PROPRIETAIRE', TRUE, '2026-10-02 09:30:00'),
    (3, 'contact@immo-tana.mg', '$2a$12$dhyfvTpmj.H.GLzfIj9U5uorcu8entf8AlD7AM.Fre/6FNYrS9Dpe', 'Razafy', 'Nirina', 'PROPRIETAIRE', TRUE, '2026-10-03 10:15:00'),
    (4, 'randria.fianar@gmail.com', '$2a$12$dhyfvTpmj.H.GLzfIj9U5uorcu8entf8AlD7AM.Fre/6FNYrS9Dpe', 'Randria', 'Paul', 'PROPRIETAIRE', TRUE, '2026-10-04 11:00:00'),
    (5, 'jhonny.etudiant@emit.mg', '$2a$12$dhyfvTpmj.H.GLzfIj9U5uorcu8entf8AlD7AM.Fre/6FNYrS9Dpe', 'Rabe', 'Jhonny', 'LOCATAIRE', TRUE, '2026-10-05 14:20:00'),
    (6, 'famille.rasoa@yahoo.fr', '$2a$12$dhyfvTpmj.H.GLzfIj9U5uorcu8entf8AlD7AM.Fre/6FNYrS9Dpe', 'Rasoa', 'Marie', 'LOCATAIRE', TRUE, '2026-10-06 16:45:00');

-- 4. Profils proprietaires / agences
INSERT INTO proprietaire (id, utilisateur_id, type, nom_agence, telephone, email_contact, quartier_id) VALUES
    (1, 2, 'PARTICULIER', NULL, '+261 34 11 222 33', 'rakoto.contact@gmail.com', 7),
    (2, 3, 'AGENCE', 'Agence Immobilière Tanà Prestige', '+261 32 02 333 44', 'contact@immo-tana.mg', 8),
    (3, 4, 'PARTICULIER', NULL, '+261 33 44 555 66', 'randria.paul@gmail.com', 1);

-- 5. Annonces (Types : 1=HOTEL, 2=ETAGE, 3=PIECE)
INSERT INTO annonce (id, proprietaire_id, type_location_id, quartier_id, titre, description, prix, surface, nb_pieces, latitude, longitude, statut, date_publication) VALUES
    -- Fianarantsoa
    (1, 3, 3, 1, 'Chambre étudiante meublée proche campus EMIT Andrainjato',
     'Idéal pour étudiant ou stagiaire. Chambre lumineuse avec lit, table de travail, armoire et accès direct à la cour. Eau courante et électricité incluses. À seulement 5 minutes à pied de l''EMIT.',
     80000.00, 16, 1, -21.4680, 47.1095, 'ACTIVE', '2026-10-10 10:00:00'),

    (2, 3, 2, 2, 'Étage de villa F4 lumineux et sécurisé à Ampasambazaha',
     'Grand étage de villa comprenant 3 chambres, un vaste séjour ensoleillé, une cuisine carrelée et 2 salles d''eau. Parking sécurisé pour 2 voitures, quartier calme et très accessible.',
     450000.00, 95, 4, -21.4530, 47.0860, 'ACTIVE', '2026-10-11 11:30:00'),

    (3, 3, 1, 5, 'Studio meublé avec kitchenette - Résidence Isaha',
     'Studio tout confort pour court ou moyen séjour à Fianarantsoa. Lit double, kitchenette équipée, eau chaude, connexion Wi-Fi haut débit disponible, service de ménage sur demande.',
     350000.00, 32, 1, -21.4605, 47.0915, 'ACTIVE', '2026-10-12 14:00:00'),

    (4, 3, 3, 4, 'Chambre économique avec sanitaire intérieur Tanambao',
     'Chambre propre et indépendante à Tanambao, sanitaire privé (douche et WC). Compteur JIRAMA divisionnaire, quartier très commerçant et bien desservi par les transports.',
     70000.00, 14, 1, -21.4425, 47.0795, 'ACTIVE', '2026-10-13 09:15:00'),

    (5, 3, 3, 3, 'Chambre calme avec balcon Ambalapaiso',
     'Grande chambre au 1er étage avec balcon privatif donnant sur jardin. Eau et gardiennage inclus. Proche de la gare et des commerces.',
     95000.00, 20, 1, -21.4490, 47.0825, 'LOUEE', '2026-10-08 08:30:00'),

    -- Antananarivo
    (6, 1, 3, 6, 'Chambre pour étudiant calme à Ankatso proche université',
     'Chambre dans pavillon partagé entre étudiants sérieux. Coin bureau, lit 1 place, placard intégré. Sanitaires partagés bien entretenus. Ambiance studieuse garantie.',
     120000.00, 18, 1, -18.9180, 47.5560, 'ACTIVE', '2026-10-14 15:45:00'),

    (7, 2, 2, 8, 'Appartement F3 haut standing à Ivandry',
     'Superbe appartement F3 moderne dans une résidence gardée 24h/24. Salon avec baie vitrée, cuisine américaine équipée, suite parentale climatisée, groupe électrogène et cuve de secours.',
     1800000.00, 115, 3, -18.8755, 47.5235, 'ACTIVE', '2026-10-15 16:20:00'),

    (8, 2, 1, 7, 'Suite meublée grand confort au coeur d''Analakely',
     'Emplacement exceptionnel en plein centre historique. Chambre de prestige avec salon, climatisation réversible, Wi-Fi fibre optique, télévision connectée. Idéal missionnaires et expatriés.',
     950000.00, 45, 2, -18.9060, 47.5260, 'ACTIVE', '2026-10-16 10:40:00'),

    (9, 1, 2, 9, 'Maison familiale F5 avec cour clôturée à Itaosy',
     'Grande maison indépendante idéale pour famille nombreuse : grand séjour avec cheminée, 4 chambres, cuisine fermée, jardin avec pelouse et arbres fruitiers, garage pour 1 voiture.',
     750000.00, 140, 5, -18.9315, 47.4725, 'ACTIVE', '2026-10-17 12:10:00'),

    (10, 2, 2, 10, 'Villa de prestige contemporaine avec piscine Talatamaty',
     'Propriété d''exception à 5 minutes de l''aéroport international d''Ivato. 5 chambres ensuite, salon de 80 m², cuisine de chef, piscine à débordement, loge de gardiennage et forage.',
     2800000.00, 240, 6, -18.8355, 47.4685, 'ACTIVE', '2026-10-17 17:00:00'),

    (11, 1, 2, 7, 'Offre suspecte prix dérisoire pour test modération',
     'Magnifique penthouse 10 pièces à Analakely pour seulement 10000 Ar par mois. Paiement par avance exigé.',
     10000.00, 300, 10, -18.9050, 47.5250, 'ACTIVE', '2026-10-18 09:00:00');

-- 6. Photos des annonces (Unsplash photos stables et haute qualite)
INSERT INTO photo (id, annonce_id, url, principale, ordre) VALUES
    -- Annonce 1 (Chambre EMIT)
    (1, 1, 'https://images.unsplash.com/photo-1522771739844-6a9f6d5f14af?auto=format&fit=crop&w=800&q=80', TRUE, 1),
    (2, 1, 'https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=800&q=80', FALSE, 2),
    -- Annonce 2 (Etage Ampasambazaha)
    (3, 2, 'https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?auto=format&fit=crop&w=800&q=80', TRUE, 1),
    (4, 2, 'https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?auto=format&fit=crop&w=800&q=80', FALSE, 2),
    (5, 2, 'https://images.unsplash.com/photo-1484154218962-a197022b5858?auto=format&fit=crop&w=800&q=80', FALSE, 3),
    -- Annonce 3 (Studio Isaha)
    (6, 3, 'https://images.unsplash.com/photo-1598928506311-c55ded91a20c?auto=format&fit=crop&w=800&q=80', TRUE, 1),
    (7, 3, 'https://images.unsplash.com/photo-1512917774080-9991f1c4c750?auto=format&fit=crop&w=800&q=80', FALSE, 2),
    -- Annonce 4 (Chambre Tanambao)
    (8, 4, 'https://images.unsplash.com/photo-1598928506311-c55ded91a20c?auto=format&fit=crop&w=800&q=80', TRUE, 1),
    -- Annonce 5 (Chambre Ambalapaiso)
    (9, 5, 'https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=800&q=80', TRUE, 1),
    -- Annonce 6 (Chambre Ankatso)
    (10, 6, 'https://images.unsplash.com/photo-1555854877-bab0e564b8d5?auto=format&fit=crop&w=800&q=80', TRUE, 1),
    (11, 6, 'https://images.unsplash.com/photo-1513694203232-719a280e022f?auto=format&fit=crop&w=800&q=80', FALSE, 2),
    -- Annonce 7 (Appart Ivandry)
    (12, 7, 'https://images.unsplash.com/photo-1545324418-cc1a3fa10c00?auto=format&fit=crop&w=800&q=80', TRUE, 1),
    (13, 7, 'https://images.unsplash.com/photo-1600585154340-be6161a56a0c?auto=format&fit=crop&w=800&q=80', FALSE, 2),
    (14, 7, 'https://images.unsplash.com/photo-1600566753376-12c8ab7fb75b?auto=format&fit=crop&w=800&q=80', FALSE, 3),
    -- Annonce 8 (Suite Analakely)
    (15, 8, 'https://images.unsplash.com/photo-1590490360182-c33d57733427?auto=format&fit=crop&w=800&q=80', TRUE, 1),
    (16, 8, 'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?auto=format&fit=crop&w=800&q=80', FALSE, 2),
    -- Annonce 9 (Maison Itaosy)
    (17, 9, 'https://images.unsplash.com/photo-1580587771525-78b9dba3b914?auto=format&fit=crop&w=800&q=80', TRUE, 1),
    (18, 9, 'https://images.unsplash.com/photo-1512918728675-ed5a9ecdebfd?auto=format&fit=crop&w=800&q=80', FALSE, 2),
    -- Annonce 10 (Villa Talatamaty)
    (19, 10, 'https://images.unsplash.com/photo-1613490493576-7fde63acd811?auto=format&fit=crop&w=800&q=80', TRUE, 1),
    (20, 10, 'https://images.unsplash.com/photo-1512915922686-57c11dde9b6b?auto=format&fit=crop&w=800&q=80', FALSE, 2),
    -- Annonce 11 (Suspecte)
    (21, 11, 'https://images.unsplash.com/photo-1512917774080-9991f1c4c750?auto=format&fit=crop&w=800&q=80', TRUE, 1);

-- 7. Favoris de test (Jhonny Rabe a mis en favoris les annonces 1 et 2)
INSERT INTO favori (utilisateur_id, annonce_id, date_ajout) VALUES
    (5, 1, '2026-10-15 11:00:00'),
    (5, 2, '2026-10-16 14:30:00');
