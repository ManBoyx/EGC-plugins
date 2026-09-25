# Dépendances et licences

Le code du dépôt est sous licence **MIT**. Voici ce dont il dépend, et sous quelle licence.
Statut « livré » = présent dans le jar distribué ; « compilation » = utilisé pour compiler seulement ; « test » = jamais distribué.

| Dépendance | Version | Licence | Usage | Livré ? |
| --- | --- | --- | --- | --- |
| Spigot API (Bukkit) | 1.8.8-R0.1-SNAPSHOT | GPL-3.0 (API Bukkit/Spigot) | Compilation | **Non** (fournie par le serveur) |
| VaultAPI | 1.7 | LGPL-3.0 | Compilation (prévu pour un futur fournisseur d'économie) | **Non** |
| JUnit Jupiter | 5.11.3 | EPL-2.0 | Tests | Non |
| Gradle Shadow (`com.gradleup.shadow`) | 8.3.5 | Apache-2.0 | Construction du jar | Non (outil) |
| Gradle | 8.10.2 | Apache-2.0 | Construction | Non (wrapper versionné) |
| Serveurs Paper / Folia | selon test | GPL-3.0 + conditions de Mojang | Tests de fumée seulement | **Non** (téléchargés, jamais versionnés) |

## Point d'attention : l'API Spigot est sous GPL-3.0

Le plugin est **compilé contre** l'API Spigot mais ne l'embarque pas : le serveur la fournit à l'exécution. Il est courant et toléré
de publier un plugin Bukkit sous une licence non-GPL dans ces conditions, mais **l'interprétation de la GPL pour les plugins
n'est pas tranchée juridiquement**. Si vous voulez redistribuer ce code dans un produit commercial, faites valider ce point.
Ce dépôt ne prétend pas trancher.

## Ce qui est livré dans le jar

Uniquement le code de ce dépôt (`libs/common`, `libs/compat`, `libs/nms`, `plugins/ultimate-core`), sans aucune bibliothèque tierce
(vérifié : le jar ne contient aucune classe `org/bukkit`, `net/md_5` ni `com/google`).
