-- Correct the moderation marker for the demo listing hidden by V4.
-- V3 ran before V4 changed this listing from ACTIVE to MASQUEE, so V3 could
-- not mark it. Add a new migration rather than modifying an applied migration.
UPDATE annonce
SET masquee_par_admin = TRUE
WHERE titre = 'Offre suspecte prix derisoire pour test moderation'
  AND statut = 'MASQUEE'
  AND masquee_par_admin = FALSE;
