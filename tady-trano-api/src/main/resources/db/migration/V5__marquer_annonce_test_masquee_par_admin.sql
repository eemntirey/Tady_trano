-- Tady Trano - Corriger le marquage de moderation de l'annonce de demonstration.
--
-- V3 marque comme masquees par l'administration les annonces deja masquees.
-- V4 masque ensuite l'annonce de test, qui etait ACTIVE lors de V3.
-- Cette migration corrige cet ordre sans modifier les migrations deja publiees.
-- Le predicat rend l'operation sans effet si le marquage est deja present.
UPDATE annonce
SET masquee_par_admin = TRUE
WHERE titre = 'Offre suspecte prix derisoire pour test moderation'
  AND statut = 'MASQUEE'
  AND masquee_par_admin = FALSE;
