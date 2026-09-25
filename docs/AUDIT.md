# Audit des plugins Minecraft existants

Date de l'audit : 25 septembre 2026, 09 h 40 (heure du serveur).
Périmètre : la machine qui héberge minebed.fr (Debian, noyau 6.1) et tout ce qui s'y trouve.

Légende : **[vérifié]** = constaté sur la machine pendant l'audit ; **[à vérifier]** = connaissance générale ou
déduction, non contrôlable d'ici. Rien n'est présenté comme vérifié s'il ne l'est pas.

## 1. Résumé

| Question | Réponse |
| --- | --- |
| Y a-t-il des plugins Minecraft (.jar Bukkit/Spigot/Paper, mods Forge/Fabric) sur la machine ? | **Non** [vérifié] |
| Y a-t-il un serveur Minecraft qui tourne sur la machine ? | **Non** [vérifié] |
| Que trouve-t-on à la place ? | Le **site** minebed.fr (Azuriom 1.2.13) et ses 6 plugins **web** (PHP) [vérifié] |
| Quelque chose est-il réutilisable comme code de plugin Minecraft ? | **Non** : les plugins Azuriom sont du PHP/Laravel, pas du Java |
| Que faut-il donc faire ? | Repartir de zéro, en respectant ce que le site attend du serveur de jeu (§4) |

### Comment l'audit a été fait

- Recherche sur tout le système de fichiers (hors `/proc`, `/sys`, `node_modules`) des descripteurs `plugin.yml`,
  `paper-plugin.yml`, `bungee.yml`, `velocity-plugin.json`, `fabric.mod.json`, `mods.toml`, `neoforge.mods.toml` : **aucun résultat**.
- Recherche des dossiers `plugins/` et `mods/` : seuls ceux d'Azuriom (web) et ceux du présent dépôt existent.
- Recherche de processus Java de type serveur (Paper, Spigot, Purpur, etc.) : **aucun**.
- Lecture des manifestes (`plugin.json`, `composer.json`) des plugins web Azuriom.

Le serveur de jeu de Minebed est donc hébergé **ailleurs** (l'API `/api/azlink` du site est prête à le recevoir).
Tout ce qui concerne les plugins qui y sont installés (AzLink, LibertyBans, Tebex…) reste **[à vérifier]** sur ce serveur.

## 2. Inventaire

### 2.1 Plugins web Azuriom (site minebed.fr) [vérifié]

Azuriom **1.2.13**, dossier `plugins/` :

| Plugin | Version | API Azuriom | Rôle | Licence déclarée |
| --- | --- | --- | --- | --- |
| contact | 1.0.2 | 1.1.0 | Formulaire de contact | non renseignée |
| forum | 1.1.10 | 1.2.0 | Forum complet | non renseignée |
| libertybans | 0.1.2 | 1.0.0 | Affiche les sanctions LibertyBans sur le site | non renseignée (fichier LICENSE présent) |
| mypurchases | 1.0.0 | 1.1.0 | Achats Tebex de l'utilisateur | non renseignée |
| tebex | 2.2.0 | 1.1.0 | Boutique Tebex | non renseignée |
| vote | 1.2.11 | 1.0.0 | Récompenses de vote | MIT |

Ce sont des extensions **du site**, pas du jeu : ni Bukkit, ni Forge, ni Fabric ne sont concernés.

### 2.2 Ce que le site attend du serveur de jeu

- **Liaison AzLink** [vérifié côté site] : l'API `GET /api/azlink` est protégée par un jeton de serveur (`server.token`) ;
  l'administration propose la vérification « AzLink » d'un serveur. Le jeton se saisit dans la configuration du plugin
  AzLink **sur le serveur de jeu** : il ne doit **jamais** figurer dans un dépôt.
- **Récompenses de vote** [vérifié côté site] : le plugin Vote envoie au serveur des **commandes console** contenant des
  jetons : `{player}` (pseudo), et `{reward}`, `{reward_id}`, `{site}`, `{site_id}` pour les commandes de récompense.
  Conséquence directe pour la conception : **toute commande d'un plugin maison doit pouvoir s'exécuter depuis la console**, avec
  un pseudo passé en argument, sans exception quand le joueur est absent.
- **Sanctions** [vérifié côté site] : le plugin `libertybans` d'Azuriom lit les données de LibertyBans ; la page de bannissement
  attend donc que **LibertyBans** soit installé côté jeu **[à vérifier]** (voir mémoire du projet : étapes en base de données
  restant à faire par l'administrateur).
- **Boutique** : Tebex livre ses achats par un plugin Tebex côté jeu **[à vérifier]**.

### 2.3 Plugins Minecraft attendus côté jeu [à vérifier]

| Plugin | Rôle | Éléments à contrôler sur le serveur de jeu |
| --- | --- | --- |
| AzLink | Liaison site ↔ jeu (vote, boutique, statut) | Plateforme (Bukkit/Paper/Folia/Bungee/Velocity), version, jeton en place |
| LibertyBans | Bannissements | Version, base de données partagée avec le site, Java requis |
| Tebex | Livraison des achats | Clé secrète en place, plateforme |

## 3. Compatibilité (cible du plugin `ultimate-core` créé dans ce dépôt)

Rien d'existant à évaluer : ce tableau décrit **ce que le nouveau plugin vise**, et ce qui est hors de portée.

| Plateforme | Statut visé | Remarques |
| --- | --- | --- |
| **Bukkit** (CraftBukkit) | Compatible par conception | API seulement ; le minimum est l'API 1.8.8 |
| **Spigot** | Compatible | Cible de compilation (API Spigot 1.8.8) |
| **Paper** et dérivés (Purpur, Pufferfish…) | Compatible | Testé sur de vrais serveurs Paper, voir `docs/COMPATIBILITY.md` |
| **Folia** | Compatible, avec réserves | Planificateur régionalisé utilisé par réflexion ; téléportations via `teleportAsync` ; modules non sûrs en multi-fil désactivés ou adaptés |
| **BungeeCord / Velocity** | **Non concerné** | Ce sont des mandataires, avec d'autres API ; hors périmètre de ce plugin |
| **Forge / NeoForge / Fabric** | **Impossible avec le même jar** | Ces chargeurs n'exposent pas l'API Bukkit. Un port demande un autre module, avec l'API du chargeur. Les hybrides (Mohist, Arclight…) ne sont ni visés ni testés |
| **Sponge** | Non concerné | API différente |

Les limites techniques précises (versions du jeu, Java, API changeantes) sont dans `docs/COMPATIBILITY.md`.

## 4. Risques

| # | Risque | Gravité | Détail / parade |
| --- | --- | --- | --- |
| R1 | Aucun plugin Minecraft n'est présent à auditer | Information | Impossible de juger un code absent ; l'audit s'arrête au côté web |
| R2 | Le plugin web `libertybans` est jeune (0.1.2), tiers, sans licence déclarée | Moyenne | Vérifier sa maintenance et sa licence avant toute dépendance durable |
| R3 | Plusieurs plugins web sans licence déclarée | Faible | Sans licence, pas de droit de réutilisation clair : ne pas copier leur code dans ce dépôt (aucun ne l'a été) |
| R4 | Jeton AzLink et clés Tebex : secrets | Élevée si fuite | Jamais dans un dépôt ; `.gitignore` et `SECURITY.md` le rappellent ; contrôle automatique dans `scripts/check-secrets.sh` |
| R5 | Commandes de récompense exécutées par la console | Moyenne | Un pseudo mal formé ou une commande mal protégée pourrait être détourné : les commandes du plugin valident tout argument |
| R6 | Ruptures de l'API Bukkit entre versions (Material, Enchantment, Sound, InventoryView…) | Élevée pour un jar unique | Traitées par une couche de compatibilité par réflexion (`libs/compat`), voir `docs/COMPATIBILITY.md` |
| R7 | Java : 8 (jusqu'à 1.16), 16 (1.17), 17 (1.18–1.20.4), 21 (1.20.5+) | Moyenne | Le jar est compilé pour Java 8 : il tourne sur toutes ces versions |
| R8 | Test réel impossible sans serveur de jeu | Moyenne | Serveurs Paper/Folia de test lancés par `scripts/smoke-test.sh` |

## 5. Recommandations

1. **Repartir de zéro** avec un seul plugin robuste plutôt que de bricoler autour d'un code absent : c'est `plugins/ultimate-core`.
2. **Compiler contre la plus vieille API visée** (Spigot 1.8.8) en Java 8, et passer par la réflexion pour tout ce qui change
   de forme entre versions : un seul jar pour toutes les versions.
3. **Rendre chaque commande utilisable depuis la console** pour que les récompenses Azuriom (vote, boutique) puissent l'appeler.
4. **Ne rien coder en dur** : messages, durées, limites dans des fichiers de configuration versionnés.
5. **Sur le serveur de jeu**, contrôler et documenter : versions d'AzLink, de LibertyBans et de Tebex ; Java utilisé ; plateforme
   (Paper ou Folia) ; sauvegardes.
6. **Ne pas exposer** les jetons : rotation immédiate si l'un d'eux a déjà été publié ailleurs.

## 6. Réutilisable ou non

| Élément | Réutilisable ? | Pourquoi |
| --- | --- | --- |
| Plugins web Azuriom | Non pour ce dépôt | PHP, autre projet, licences non déclarées (sauf Vote : MIT) |
| Jetons `{player}`, `{reward}`… du plugin Vote | **Oui, comme contrat** | `ultimate-core` accepte les commandes console avec ces valeurs |
| Serveur de jeu actuel | Inconnu | Non accessible depuis cette machine |
