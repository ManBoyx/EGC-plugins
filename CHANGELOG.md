# Journal des changements

## 0.1.0 — première version

Premier jet d'UltimateCore et de l'infrastructure du dépôt.

**Plugin**
- Modules : spawn, maisons, points de passage, demandes de téléportation, retour, kits, économie interne, chat
  (anti-spam, répétitions, majuscules, mots interdits, liens), utilitaires, connexion, annonces, pages d'information.
- Commande `/uc` : aide, rechargement, informations, auto-test, annonce, titre, barre d'action, son.
- Un seul jar (Java 8, API Spigot 1.8.8) pour Bukkit, Spigot, Paper et Folia, de la 1.8 à la 26.x.
- Messages en français et en anglais ; aucune télémétrie, aucun appel réseau.

**Infrastructure**
- Modules Gradle : `libs/common`, `libs/compat`, `libs/nms`, `plugins/ultimate-core`, conventions dans `build-logic`.
- Tests unitaires (logique pure), auto-test embarqué, test de fumée sur de vrais serveurs Paper et Folia,
  joueurs simulés (mineflayer), contrôle de secrets, intégration continue.
- Documentation : audit, architecture, compatibilité, dépendances, feuille de route, règles du projet.

**Corrigé pendant la mise au point** (constaté avec de vrais serveurs et joueurs simulés)
- `/back` ne fonctionnait pas sur Folia : l'événement de téléportation n'y est pas émis pour `teleportAsync`.
  Le service de téléportation enregistre maintenant lui-même le point de départ.
- `/pay` et `/eco` : un « début de pseudo » unique suffisait à désigner un joueur, au risque de payer la mauvaise personne.
  Le pseudo doit maintenant être exact.
- Paper 1.8 (ancien « PaperSpigot ») était étiqueté « Spigot ».
