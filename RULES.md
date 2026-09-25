# Règles du projet

Ce fichier réunit les règles données au lancement du projet (§1) et celles qui ont dû être décidées en cours de route
parce qu'elles n'étaient pas écrites (§2 à §6). Une règle qui n'est pas ici n'existe pas : ajoutez-la.

## 1. Règles données au lancement

1. **Priorités** : build multi-version > cœur du plugin fonctionnel > fonctionnalités > documentation.
2. **Commits atomiques**, un sujet chacun.
3. **Jamais de suppression de code sans sauvegarde** (branche ou copie) : on archive, on ne détruit pas.
4. **Aucun secret** (jeton, clé, mot de passe) dans le dépôt.
5. **Licence MIT** pour le code du dépôt ; **vérifier la licence de chaque dépendance** (voir `docs/DEPENDENCIES.md`).
6. **Documenter avec précision les limites de compatibilité** impossibles à lever (voir `docs/COMPATIBILITY.md`).
7. Toute règle non explicite décidée en cours de route est consignée ici.

## 2. Compatibilité (règles techniques décidées)

- **C1. Un seul jar** pour toutes les versions : compilé en Java 8 (`options.release = 8`) contre l'API **Spigot 1.8.8**.
  Toute API plus récente est appelée par réflexion, jamais directement.
- **C2. Ne jamais appeler directement** une méthode statique ou de fabrique de `Enchantment`, `Sound`, `PotionEffectType`,
  `Biome`, `Attribute`… : ces classes sont devenues des interfaces ou ont changé de forme dans les versions récentes, ce qui
  produit `IncompatibleClassChangeError` ou `NoSuchMethodError` **à l'exécution seulement**. Passer par `libs/compat`
  (`Enchants`, `Sounds`, `Materials`).
- **C3. Pas de `InventoryView`** ni de menus à inventaire tant que `libs/compat` ne les encapsule pas : `InventoryView` est
  devenue une interface en 1.21.
- **C4. Matériaux** : toujours par nom, via `Materials.resolve` (accepte les noms d'avant et d'après la 1.13). Pas de valeurs de
  données (« WOOL:14 »).
- **C5. Planificateur** : uniquement `Scheduler` (`libs/compat`), jamais `Bukkit.getScheduler()` en dehors de `BukkitScheduler`.
  Toute action sur un joueur depuis une autre source que sa propre commande passe par `Ctx.runOnPlayer`.
- **C6. Téléportation** : uniquement `Teleports.teleport` (ou `TeleportService`), pour que Folia fonctionne.
- **C7. Pas de recherche de joueur hors ligne** par `Bukkit.getOfflinePlayer(String)` : elle peut bloquer le serveur sur une
  requête réseau. On n'utilise que les joueurs en ligne et les pseudos déjà vus, enregistrés par le plugin.
- **C8. Aucun accès réseau** dans le plugin ; aucune télémétrie.
- **C9. Version minimale du jeu : 1.8** (`Bukkit.getOnlinePlayers()` y renvoie une collection ; avant, un tableau).
- **C10. Couleurs hexadécimales** : natives à partir de la 1.16 ; avant, on prend la couleur classique la plus proche.

## 3. Comportement du plugin

- **P1. Toute commande doit fonctionner depuis la console** avec un pseudo en argument, pour les récompenses du site.
- **P2. Une erreur dans une commande ne doit jamais afficher de trace à un joueur** : message générique, détail dans le journal.
- **P3. Une erreur dans un module ne doit pas empêcher les autres de démarrer.**
- **P4. Aucun message en dur** : tout texte affiché vient de `lang/*.yml`. Chaque clé existe dans toutes les langues
  avec les mêmes jetons (vérifié par test).
- **P5. Les noms saisis** (maisons, points de passage, kits) : `[A-Za-z0-9_-]{1,24}`.
- **P6. Les montants d'argent** sont des entiers de centimes ; jamais de `double` pour stocker un solde.
- **P7. Les fichiers de données** s'écrivent hors du fil principal, de façon atomique, et un fichier illisible est mis de côté,
  jamais écrasé.
- **P8. Une option de configuration absente prend sa valeur par défaut** ; la configuration porte un numéro de version.
- **P9. Les données de joueur sont clés par UUID**, jamais par pseudo (un pseudo change).

## 4. Tests

- **T1. La logique qui n'a pas besoin de Bukkit** va dans une classe pure et a des tests unitaires.
- **T2. Chaque changement de compatibilité** est validé sur un vrai serveur (`scripts/smoke-test.sh`), au moins sur une version
  ancienne et une récente.
- **T3. Un test ne se contente pas de passer** : on vérifie qu'il peut échouer (le contrôle de secrets, par exemple, a été essayé
  sur de faux secrets).
- **T4. Ce qui n'est pas testé est écrit comme tel** dans `docs/COMPATIBILITY.md`.

## 5. Serveurs de test et EULA

- **S1. Le test de fumée n'accepte pas l'EULA de Mojang à la place de l'utilisateur** : il faut exporter `HUB_ACCEPT_EULA=true`.
- **S2. Les serveurs de test n'écoutent que sur `127.0.0.1`**, en mode hors ligne, 4 joueurs au plus, et sont arrêtés à la fin.
- **S3. Les jars de serveur ne sont jamais versionnés** (`.smoke/` est ignoré) ; ils sont téléchargés avec contrôle SHA-256.
- **S4. Le test de fumée refuse de démarrer** si la mémoire disponible est trop basse, pour ne pas gêner une machine qui héberge
  autre chose (`HUB_FORCE=1` pour passer outre).

## 6. Dépôt

- **D1. Messages de commit en français.**
- **D2. Le dépôt reste local/privé** tant que son propriétaire n'a pas décidé de le publier.
- **D3. Les fichiers générés** (`build/`, `.gradle/`, `.smoke/`, `run/`) ne sont jamais suivis.
