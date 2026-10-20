# Audit UX et bugs du client Tady Trano

**Date :** 20 octobre 2026  
**Périmètre :** client JavaFX — recherche, authentification, thème, navigation et images  
**Méthode :** inspection statique des vues, styles CSS, navigation et gestionnaire d'images. La validation visuelle sur un poste Windows n'est pas simulée par la CI.

## Évaluation générale

L'interface était fonctionnelle mais pas encore suffisamment homogène pour un usage professionnel. La recherche utilisait des cartes étroites, plusieurs textes secondaires étaient peu lisibles en thème sombre et l'écran d'authentification ressemblait à un formulaire technique plutôt qu'à un écran d'accueil professionnel. La navigation et l'aide dépendaient trop du contexte de la vue.

## Anomalies et remédiations

| Priorité | Constat avant correction | Impact utilisateur | Remédiation proposée dans cette branche |
|---|---|---|---|
| Haute | La feuille CSS utilisait une police globale de 13 px et plusieurs libellés de 10–12 px ; les styles « muted » reprenaient la couleur atténuée du thème sans adaptation au thème sombre. Les boutons de navigation neutralisaient aussi leur bordure lorsqu'ils recevaient le focus. | Lecture difficile, surtout en mode sombre et au clavier. | Augmentation des tailles utiles, couleurs secondaires dédiées aux thèmes clair/sombre et focus clavier visible. |
| Haute | L'écran connexion/inscription était une simple colonne de champs et de boutons, limitée à une largeur de formulaire d'environ 420 px, sans mise en page d'accueil ni accès au support. | Première impression peu professionnelle et assistance difficile à trouver. | Présentation à deux colonnes, panneau de présentation, formulaire hiérarchisé, champs étiquetés, validation par Entrée et accès support. |
| Haute | Le retour depuis une annonce dépendait d'une fonction de retour passée à la vue détail ; sans callback, le bouton pouvait échouer. Il n'existait pas de retour global dans la navigation. | Retour incohérent selon le point d'entrée et risque de revenir vers une vue périmée. | Historique de vues central, bouton Retour global, retour de détail sans empiler la même vue une seconde fois et remise à zéro de l'historique après déconnexion/connexion. |
| Haute | Aucun bouton global « Aide / contact » n'était défini dans la barre de navigation. | Aucun chemin évident pour signaler un bug ou contacter l'équipe depuis une vue autre que le détail d'une annonce. | Ajout d'un bouton « Aide » ouvrant le formulaire de signalement GitHub du projet, et d'un bouton de support sur l'écran de connexion. |
| Moyenne | Les cartes de recherche étaient limitées à 190 × 112 px pour l'image ; les titres pouvaient être plafonnés à 36 px de haut. | Annonces moins faciles à comparer, titres et photos visuellement comprimés. | Cartes élargies à 230 px, images plus hautes, coins arrondis et hauteur de titre ajustée. |
| Moyenne | Chaque nouvelle instance de la vue Recherche rechargeait villes, quartiers et types via des appels REST séquentiels. | Temps d'attente et trafic réseau supplémentaires en revenant à la recherche. | Mise en cache en mémoire des référentiels immuables pour éviter les appels répétitifs pendant la session. |
| Moyenne | Quand le chargement asynchrone d'une image échouait, le cache en erreur était nettoyé et un placeholder conservé, mais aucune nouvelle tentative n'était effectuée pour une erreur transitoire. | Certaines photos pouvaient rester en placeholder après un incident réseau temporaire. | Une nouvelle tentative différée, limitée à une fois, uniquement si la même URL est encore demandée. Le placeholder reste le repli en cas d'échec définitif. |
| Faible | Le chargement des styles CSS supposait que la ressource existait et appelait directement `toExternalForm()`. | Une ressource CSS absente pouvait provoquer une exception au changement de thème. | Vérification explicite de la présence de la ressource avant son ajout à la scène. |

## Limites restantes

- Les photos restent hébergées par des URL externes. Une URL supprimée, un hébergeur indisponible ou une connexion bloquée ne peut pas être réparé par le client seul ; le placeholder doit rester visible. Un hébergement d'images avec validation serveur et stockage durable serait une évolution distincte.
- L'audit statique et la CI ne remplacent pas un essai manuel de l'application JavaFX sous Windows, notamment pour les dimensions d'écran, le mode sombre, les liens vers le navigateur et les images réelles.
- Les anomalies backend précédemment auditées ne sont pas toutes incluses dans ce périmètre UX.

## Validation

La CI GitHub doit réussir après ces modifications. Les jobs Maven et MySQL/Flyway valident la compilation, les tests et les migrations ; ils ne font pas de capture visuelle ou de test JavaFX de bout en bout.
