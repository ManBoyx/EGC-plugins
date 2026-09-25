# Architecture

## Vue d'ensemble

```
                       ┌─────────────────────────────┐
                       │      plugins/ultimate-core   │   le jar livré (Shadow)
                       │  UltimatePlugin · CoreModule │
                       │  modules : spawn homes warps │
                       │  tpa back kits economy chat  │
                       │  utility join announcer info │
                       └───────┬───────────┬─────────┘
                               │           │
                    ┌──────────▼───┐  ┌────▼──────────┐
                    │ libs/compat  │  │ libs/common   │
                    │ Platform     │  │ (pur, sans    │
                    │ Scheduler    │  │  Minecraft)   │
                    │ Materials…   │  └───────────────┘
                    └──────┬───────┘
                           │
                    ┌──────▼───────┐
                    │  libs/nms    │   réflexion seulement
                    └──────────────┘
```

- `libs/common` ne dépend de rien : couleurs, jetons, durées, montants, filtres de chat, temps de recharge, limiteur, versions.
  Il pourrait servir tel quel à un futur port Fabric ou Forge.
- `libs/nms` : accès aux détails internes du serveur (titre et barre d'action des vieux serveurs, ping) **par réflexion**.
  Il n'y a **pas un module par version de Minecraft** : c'est ce qui permet un seul jar.
- `libs/compat` : ce qui change selon la plateforme (Bukkit, Spigot, Paper, Folia) ou la version du jeu.
- `plugins/ultimate-core` : le plugin proprement dit.

## Le principe de compatibilité

Le jar est compilé en Java 8 contre l'API **Spigot 1.8.8** : c'est la plus ancienne API visée, donc tout ce que le code appelle
directement existe partout. Les pièges sont les classes dont **la forme change** plus tard (une énumération devenue interface :
l'appel compile, puis casse à l'exécution). Elles sont encapsulées dans `libs/compat` et appelées par réflexion. Voir `RULES.md` (C1 à C10).

## Modules

Un module (`Module`, en pratique `AbstractModule`) regroupe des commandes, des écouteurs et des données. `UltimatePlugin` les
démarre selon `modules:` dans `config.yml`, isole leurs erreurs, et sait les redémarrer (`/uc reload`). Les commandes d'un
module coupé ou en échec répondent « fonction désactivée » (`DisabledCommand`).

## Fils d'exécution (Folia)

| Situation | Règle |
| --- | --- |
| Commande ou événement d'un joueur | Déjà dans la bonne région : on agit directement sur ce joueur |
| Action sur **un autre** joueur (`/heal Bob` lancé par Alice, ou par la console) | `Ctx.runOnPlayer(bob, …)` : sur Folia, planifiée dans la région de Bob |
| Téléportation | `Teleports.teleport` : `teleportAsync` quand il existe (Paper 1.13+, Folia), sinon `teleport` sur le bon fil |
| Écriture de fichiers | `DataStore` : hors du fil principal, atomique, regroupée (une écriture toutes les 2 s au plus) |
| Événement de chat | Asynchrone : uniquement du calcul pur (`ChatGuard`), aucun appel à l'API du jeu |

Sur Bukkit, Spigot et Paper, `runOnPlayer` exécute tout de suite (on est déjà sur le fil principal).

## Données

Tout est dans `plugins/EGC-plugins/` :

| Fichier | Contenu |
| --- | --- |
| `config.yml` | Réglages (versionné : `config-version`) |
| `lang/fr.yml`, `lang/en.yml` | Messages |
| `homes.yml` | `<uuid>.<nom>` → emplacement |
| `warps.yml`, `spawn.yml` | Points de passage, spawn |
| `economy.yml` | Soldes en centimes, pseudos déjà vus |
| `kit-cooldowns.yml` | Fin de recharge par joueur et par kit |

Le format d'emplacement est `monde;x;y;z;lacet;tangage` (`SavedLocation`). YAML est un choix de simplicité : un stockage
SQLite/MySQL figure dans la feuille de route.

## Messages

`Msg` (énumération) liste toutes les clés ; `Messages` cherche d'abord dans le fichier de l'administrateur, puis dans la même
langue du jar, puis en français du jar : une mise à jour qui ajoute des messages ne laisse jamais de clé manquante.

## Auto-test

`/uc selftest` exécute, sur le serveur réel, les vérifications qui dépendent de la version : matériaux, enchantements, sons, objets,
messages, fichiers, commandes, planificateur. Il sert au test de fumée (`scripts/smoke-test.sh`) et au diagnostic.
