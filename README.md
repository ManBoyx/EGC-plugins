# Minecraft Plugins Hub

Dépôt de plugins Minecraft, avec **UltimateCore** : un socle de serveur (téléportations, kits, économie, chat, utilitaires)
qui tourne avec **un seul jar** sur Bukkit, Spigot, Paper et Folia, de la **1.8** à la dernière version.

> État : version 0.1.0, première publication. Ce qui est vérifié, et ce qui ne l'est pas, est écrit noir sur blanc dans
> [docs/COMPATIBILITY.md](docs/COMPATIBILITY.md). Les limites connues sont dans [docs/ROADMAP.md](docs/ROADMAP.md).

## Ce que fait UltimateCore

| Module | Commandes | Notes |
| --- | --- | --- |
| Spawn | `/spawn [joueur]`, `/setspawn` | Nouveaux joueurs envoyés au spawn (option) |
| Maisons | `/sethome`, `/home`, `/delhome`, `/homes` | Limite par permission `ultimatecore.homes.limit.N` |
| Points de passage | `/warp`, `/warps`, `/setwarp`, `/delwarp` | Permission par point (option) |
| Demandes | `/tpa`, `/tpahere`, `/tpaccept`, `/tpdeny`, `/tpcancel` | Expirent seules |
| Retour | `/back` | Dernier endroit quitté, ou lieu de la mort |
| Kits | `/kit [nom] [joueur]`, `/kits` | Temps de recharge conservé, noms d'objets valables avant et après la 1.13 |
| Économie | `/balance`, `/pay`, `/eco`, `/baltop` | Soldes en centimes entiers, pas d'erreur d'arrondi |
| Chat | `/clearchat` + filtres | Anti-spam, répétitions, majuscules, mots interdits (même déguisés), liens |
| Utilitaires | `/heal`, `/feed`, `/fly`, `/gamemode`, `/speed`, `/ping` | Chacun accepte un joueur en argument |
| Connexion | (automatique) | Messages, première visite, message du jour, titre de bienvenue |
| Annonces | (automatique) | Annonces périodiques (désactivé par défaut) |
| Information | `/info [sujet]`, `/rules`, `/discord`, `/vote`, `/store`… | Pages écrites dans la configuration |
| Noyau | `/uc help\|version\|reload\|info\|selftest\|broadcast\|title\|actionbar\|sound` | Voir « Console et récompenses » |

Chaque module se coupe dans `config.yml`. Messages en **français** et **anglais** (`lang/`), modifiables.
**Aucune télémétrie, aucun appel réseau** : le plugin ne se connecte à rien.

## Installer

1. Récupérez `ultimate-core-<version>.jar` (voir « Compiler »).
2. Déposez-le dans le dossier `plugins/` du serveur, redémarrez.
3. Modifiez `plugins/UltimateCore/config.yml`, puis `/uc reload`.
4. Vérifiez : `/uc selftest` (console) doit finir par `SELFTEST RESULT: PASS`.

Java : celui qu'exige votre version du jeu (8 jusqu'à la 1.16, 17 pour la 1.18 à 1.20.4, 21 ensuite, 25 pour la numérotation 26.x).
Le jar est compilé pour Java 8 et tourne sur toutes ces versions.

## Console et récompenses (Azuriom, Tebex…)

Toutes les commandes marchent depuis la console, avec un pseudo en argument : les récompenses de vote ou de boutique
peuvent donc les lancer. Exemples (le jeton `{player}` est remplacé par le site) :

```
kit starter {player}
eco give {player} 250
uc title {player} &6Merci !|&7Votre vote est compté
uc broadcast &e{player} &fa voté pour le serveur !
uc sound {player} LEVEL_UP
```

Plus de détails dans [examples/azuriom-rewards.md](examples/azuriom-rewards.md).

## Compiler

Prérequis : JDK 17 ou plus (le résultat reste du Java 8). Aucune autre installation : le wrapper Gradle s'en charge.

```bash
./gradlew build          # compile et lance tous les tests
./gradlew dist           # copie le jar prêt à installer dans build/dist/
```

## Tester

```bash
./gradlew test                                                 # tests unitaires (logique pure, sans serveur)
HUB_ACCEPT_EULA=true scripts/smoke-test.sh paper 1.20.4        # vrai serveur : charge le plugin, lance /uc selftest
HUB_ACCEPT_EULA=true scripts/smoke-matrix.sh                   # toute la matrice de versions
scripts/check-secrets.sh                                       # aucun secret dans les fichiers suivis
```

Le test de fumée télécharge les serveurs Paper/Folia (jamais versionnés ici) et **n'accepte pas l'EULA de Mojang à votre place** :
c'est vous qui exportez `HUB_ACCEPT_EULA=true`.

## Organisation du dépôt

```
plugins/ultimate-core/   le plugin (modules, commandes, configuration, langues)
libs/common/             utilitaires purs, sans Minecraft (couleurs, durées, filtres, montants…)
libs/compat/             compatibilité entre versions et plateformes (planificateur Folia, matériaux, sons…)
libs/nms/                accès aux détails internes du serveur, par réflexion seulement
build-logic/             conventions de compilation partagées (Java 8, Shadow, JUnit)
docs/                    audit, architecture, compatibilité, dépendances, feuille de route
scripts/                 téléchargement de serveurs, tests de fumée, contrôle de secrets
examples/                configurations d'exemple
.github/workflows/       intégration continue
```

Voir [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) pour le détail.

## Contribuer, sécurité, règles

- [CONTRIBUTING.md](CONTRIBUTING.md) : comment proposer un changement.
- [SECURITY.md](SECURITY.md) : signaler une faille ; ce que le plugin fait (et ne fait pas) avec vos données.
- [RULES.md](RULES.md) : les règles du projet, y compris celles décidées en cours de route.

## Licence

MIT pour le code de ce dépôt ([LICENSE](LICENSE)). Les dépendances et leurs licences : [docs/DEPENDENCIES.md](docs/DEPENDENCIES.md).
