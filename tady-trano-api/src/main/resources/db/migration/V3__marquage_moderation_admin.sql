-- Distinguishes a moderation-hidden listing from one hidden by its owner.
ALTER TABLE annonce
    ADD COLUMN masquee_par_admin BOOLEAN NOT NULL DEFAULT FALSE;

-- Existing hidden listings are treated as moderation-hidden until reviewed by an admin.
UPDATE annonce
SET masquee_par_admin = TRUE
WHERE statut = 'MASQUEE';
