-- Tady Trano - Securisation des donnees de demonstration (correctif post-audit)
--
-- Cette migration est idempotente : elle peut etre reappliquee sans effet de bord.
-- Elle ne modifie PAS V2 (Flyway protege les sommes de controle des migrations deja appliquees).
--
-- 1. Desactiver les comptes de demonstration inseres par V2.
--    Ces comptes possedent tous le mot de passe publie 'Pass1234!' et ne doivent
--    pas rester actifs sur une installation exposee.
UPDATE utilisateur
SET actif = FALSE
WHERE email IN (
    'admin@tady.mg',
    'rakoto.proprio@gmail.com',
    'contact@immo-tana.mg',
    'randria.fianar@gmail.com',
    'jhonny.etudiant@emit.mg',
    'famille.rasoa@yahoo.fr'
) AND actif = TRUE;

-- 2. Masquer l'annonce de test de moderation inseree par V2 (statut ACTIVE).
--    Cette annonce ne doit pas etre visible comme une offre reale.
UPDATE annonce
SET statut = 'MASQUEE'
WHERE titre = 'Offre suspecte prix derisoire pour test moderation'
  AND statut = 'ACTIVE';
