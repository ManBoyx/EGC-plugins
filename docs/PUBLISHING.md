# Publier

Ce dépôt est prêt à être publié, mais **rien n'a été publié** : il est resté local. Voici la marche à suivre et ce qui reste à décider.

## Liste de contrôle avant publication

1. `./gradlew build` passe.
2. `scripts/check-secrets.sh` ne signale rien, **et** relisez `git log -p` une fois : aucun jeton, clé, mot de passe ou webhook,
   y compris dans d'anciens commits (une suppression ultérieure ne suffit pas : il faudrait révoquer le secret).
3. Les tests de fumée passent sur la matrice complète : `HUB_ACCEPT_EULA=true HUB_BOTS=1 scripts/smoke-matrix.sh`.
4. Relisez `docs/COMPATIBILITY.md` et `docs/ROADMAP.md` : ils disent ce qui est vérifié et ce qui ne l'est pas ; ne promettez pas plus.
5. Le nom d'auteur et l'adresse dans les commits vous conviennent (`git log --format='%an <%ae>'`).
6. `LICENSE` : MIT, au nom du titulaire voulu (actuellement « ManBoyx »).

## Créer le dépôt GitHub

```bash
gh repo create minecraft-plugins-hub --private --source . --remote origin   # privé d'abord
git push -u origin main
```

Les fichiers `.github/workflows/` demandent que votre jeton `gh` ait le droit `workflow` (`gh auth refresh -s workflow`).
Pour le test de fumée en intégration continue, créez la variable de dépôt `HUB_ACCEPT_EULA=true` **seulement si vous acceptez
l'EULA de Mojang** (https://aka.ms/MinecraftEULA).

## Produire une version

```bash
./gradlew clean build dist        # build/dist/ultimate-core-<version>.jar + SHA256SUMS
git tag -a v0.1.0 -m "UltimateCore 0.1.0"
```

Joignez le jar **et** `SHA256SUMS` à la publication (release GitHub). La version vient de `gradle.properties` (`hub.version`).

## Sites de distribution de plugins

Non faits, à votre initiative : Hangar (PaperMC), Modrinth, SpigotMC. Chacun a ses règles (description, captures, licence,
parfois relecture manuelle). Reprenez la table de `README.md` et les limites de `docs/ROADMAP.md` pour la description.

## Avant de passer en public

Relisez `docs/DEPENDENCIES.md` (point d'attention sur la licence de l'API Spigot) et décidez si vous souhaitez
faire relire cet aspect si le projet devient commercial.
