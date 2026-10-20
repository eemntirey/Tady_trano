# A-08 — Vérification manuelle anti-double-soumission

La boîte de dialogue JavaFX et son cycle de vie ne sont pas testés en mode headless dans la suite actuelle. Exécuter ce scénario avec le client lancé et un compte de rôle **PROPRIETAIRE**.

## Création
1. Ouvrir « Mes annonces » puis « Publier une annonce ».
2. Renseigner les champs obligatoires et cliquer sur « Enregistrer ».
3. Ralentir temporairement la réponse de `POST /api/annonces` (point d'arrêt dans le serveur ou délai de test), puis cliquer sur « Modifier » pour une annonce existante.
4. Vérifier que le second formulaire ne s'ouvre pas et que le message « Une opération est en cours… » apparaît. Le bouton « Publier une annonce » doit rester désactivé.
5. À la fin de la requête et du rafraîchissement, vérifier qu'une seule annonce a été créée, que la confirmation s'affiche et que les boutons sont réactivés.

## Modification
1. Ouvrir « Modifier » sur une annonce existante, changer un champ et enregistrer.
2. Pendant la requête, tenter d'ouvrir un autre formulaire de modification.
3. Vérifier que le second formulaire est bloqué et qu'une seule modification est envoyée.

## Erreur réseau
1. Soumettre une création ou une modification, puis interrompre l'API avant sa réponse.
2. Vérifier que l'erreur est signalée, que le verrou est libéré dans tous les cas et que « Publier une annonce » est réactivé.
3. Rouvrir un formulaire et confirmer que la vue reste utilisable.
