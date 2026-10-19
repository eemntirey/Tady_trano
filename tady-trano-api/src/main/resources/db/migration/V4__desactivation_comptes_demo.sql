-- Ne pas modifier V2 : Flyway doit conserver le checksum des migrations deja appliquees.
-- Le profil local utilise une base H2 volatile et des comptes de demo distincts.
-- Sur toute base persistante Flyway, desactiver les six comptes publics documentes
-- dans V2 afin qu'ils ne puissent pas etre utilises avec le mot de passe partage Pass1234!.
-- Aucune ligne ni annonce n'est supprimee : les donnees historiques restent recuperables.
UPDATE utilisateur
SET actif = FALSE
WHERE email IN (
    'admin@tady.mg',
    'rakoto.proprio@gmail.com',
    'contact@immo-tana.mg',
    'randria.fianar@gmail.com',
    'jhonny.etudiant@emit.mg',
    'famille.rasoa@yahoo.fr'
);
