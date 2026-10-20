-- V4 masque l'annonce de démonstration, mais ne marque pas toujours
-- explicitement le verrou de modération. Corriger les deux propriétés ensemble.
-- Nouvelle migration : ne jamais modifier V3/V4 déjà appliquées.
UPDATE annonce
SET statut = 'MASQUEE',
    masquee_par_admin = TRUE
WHERE titre = 'Offre suspecte prix dérisoire pour test modération';
