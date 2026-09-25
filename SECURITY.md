# Sécurité

## Signaler une faille

Ne publiez pas de faille dans un ticket public. Écrivez au propriétaire du dépôt par le canal privé indiqué sur son profil,
avec : la version du plugin, la version du serveur, et les étapes pour reproduire. Une réponse sera donnée dès que possible.

## Versions suivies

Seule la dernière version publiée reçoit des correctifs.

## Ce que fait le plugin

- **Aucun appel réseau**, aucune télémétrie, aucun mécanisme de mise à jour automatique.
- **Fichiers** : il lit et écrit uniquement dans son dossier (`plugins/EGC-plugins/`) : `config.yml`, `lang/*.yml` et les
  fichiers de données (`homes.yml`, `warps.yml`, `spawn.yml`, `economy.yml`, `kit-cooldowns.yml`). Les écritures sont atomiques ;
  un fichier de données illisible est mis de côté sous un autre nom, jamais écrasé en silence.
- **Noms saisis par les joueurs** (maisons, points de passage, kits) : limités à `[A-Za-z0-9_-]{1,24}`, donc jamais utilisés
  comme chemin de fichier ni comme clé ambiguë.
- **Montants** : comptés en centimes entiers, plafonnés, avec refus des valeurs négatives, nulles, `NaN` ou infinies.
- **Commandes lancées par la console** (récompenses du site) : les arguments sont validés comme ceux d'un joueur.
  Attention : quiconque peut écrire une récompense dans votre site peut lancer n'importe quelle commande console.
  Protégez l'accès administrateur de votre site.

## Secrets

Ce dépôt ne contient aucun secret et ne doit jamais en contenir : jetons AzLink, clés Tebex, clés d'API, mots de passe de
base de données, webhooks. `scripts/check-secrets.sh` (lancé en intégration continue) attrape les cas courants ; il ne remplace
pas la vigilance. Un secret publié par erreur doit être **révoqué**, pas seulement supprimé de l'historique.

## Chaîne de compilation

- Les dépendances viennent de dépôts officiels (Maven Central, Spigot, JitPack pour VaultAPI) ; leurs versions sont fixées dans
  `gradle/libs.versions.toml` et `build-logic`.
- Le wrapper Gradle (`gradle/wrapper/`) est versionné ; vérifiez sa somme de contrôle si vous ne faites pas confiance au dépôt :
  https://gradle.org/release-checksums/
- Les serveurs de test sont téléchargés depuis l'API officielle de PaperMC, avec vérification de la somme SHA-256.
