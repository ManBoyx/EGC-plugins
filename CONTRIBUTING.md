# Contribuer

Merci de votre aide. Ce fichier dit comment travailler sur le dépôt sans casser la compatibilité entre versions,
qui est ce qui coûte le plus cher à réparer.

## Mise en route

```bash
git clone <ce dépôt> && cd EGC-plugins
./gradlew build                 # JDK 17+ ; le résultat reste du Java 8
```

## Avant de proposer un changement

1. `./gradlew build` passe (compilation + tests unitaires).
2. `scripts/check-secrets.sh` ne signale rien.
3. Si vous touchez au chargement, aux commandes, au planificateur ou à la compatibilité : lancez au moins
   `HUB_ACCEPT_EULA=true scripts/smoke-test.sh paper <version>` sur **une ancienne** (1.8.8 ou 1.12.2) **et une récente** version.
4. Un comportement nouveau a son test. La logique qui n'a pas besoin de Bukkit va dans une classe pure (voir `libs/common`
   et `plugins/ultimate-core/.../logic`), testable sans serveur.

## Commits

- Petits et **atomiques** : un sujet par commit, le dépôt compile après chacun.
- Message en français, à l'indicatif ou au nom : « libs/compat : … », « ultimate-core : … ».
- Jamais de secret (jeton, clé, mot de passe, webhook) : ni dans le code, ni dans l'historique. Si cela arrive, le secret est
  à révoquer tout de suite, pas seulement à effacer.

## Ajouter un message

1. Ajoutez la clé dans l'énumération `Msg`.
2. Ajoutez-la dans `lang/fr.yml` **et** `lang/en.yml`, avec les mêmes `{jetons}`.
3. `./gradlew :plugins:ultimate-core:test` vérifie la cohérence (`ResourcesTest`).

## Ajouter une commande

1. Déclarez-la dans `plugin.yml` (description, usage) et ses permissions.
2. Implémentez-la dans un module (`BaseCommand`) : elle doit **marcher depuis la console** (pseudo en argument) et ne jamais
   afficher de trace à un joueur.
3. `/uc selftest` vérifie que chaque commande de `plugin.yml` a un exécuteur.

## Ajouter un module

Créez une classe qui étend `AbstractModule`, ajoutez son identifiant à `config.yml` (`modules:`), à `UltimatePlugin.enableModules`
et à `ResourcesTest`.

## Ce qu'il ne faut pas faire (compatibilité)

Le jar est compilé contre l'API 1.8.8 et doit tourner jusqu'aux versions les plus récentes. Certaines classes changent de forme
entre versions (énumération devenue interface) : les appeler directement casse le plugin à l'exécution, sans erreur à la compilation.
Les règles sont dans [RULES.md](RULES.md) (section « Compatibilité ») ; la plus importante : **passez par `libs/compat`**
pour les matériaux, enchantements, sons, titres, planificateur et téléportations.
