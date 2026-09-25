# Feuille de route et limites connues

Ce qui n'est **pas fait**, dit franchement.

## Limites connues (v0.1.0)

| Sujet | Limite |
| --- | --- |
| Tests avec joueur connecté | Le test de fumée charge le plugin sur de vrais serveurs et exécute les commandes **de la console** ; il n'y a pas encore de test de bout en bout avec un joueur (téléportation, chat, kits reçus en jeu). Cette logique est couverte par des tests unitaires de sa partie pure. |
| Titre et barre d'action des vieux serveurs (1.8 à 1.11) | Passent par des paquets internes, par réflexion : écrits d'après la structure connue de ces versions, **non vérifiés avec un joueur**. Repli automatique dans le chat si ça échoue. |
| Folia | Le chargement, le planificateur régionalisé et l'auto-test sont vérifiés ; les scénarios de jeu (plusieurs joueurs dans des régions différentes) ne le sont pas. |
| Menus à inventaire | Absents : `InventoryView` est devenue une interface en 1.21 (voir `RULES.md` C3). |
| Économie | Interne seulement ; **pas de fournisseur Vault** : les autres plugins d'économie ne la voient pas. |
| Stockage | Fichiers YAML : adapté à un serveur moyen, pas à des dizaines de milliers de joueurs. |
| Valeurs de données d'avant la 1.13 (« WOOL:14 ») | Non gérées dans les kits. |
| 1.7.10 et avant | Non pris en charge (`getOnlinePlayers` renvoie un tableau). |
| Forge, NeoForge, Fabric, Sponge, proxys (Bungee/Velocity) | Hors périmètre : autres API. |

## Idées, par ordre d'intérêt

1. Test de bout en bout avec de vrais joueurs (clients simulés) dans `scripts/`, puis en intégration continue.
2. Fournisseur Vault pour l'économie (VaultAPI 1.7 est déjà en dépendance de compilation).
3. Stockage SQLite ou MySQL en option, avec migration depuis le YAML.
4. Extension PlaceholderAPI (solde, ping…).
5. Menus à inventaire, une fois l'encapsulation de `InventoryView` écrite dans `libs/compat`.
6. Ports Fabric/NeoForge en réutilisant `libs/common`.
7. Silencieux (mute) et sanctions : volontairement laissés à LibertyBans.
