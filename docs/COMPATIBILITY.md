# Compatibilité

Ce document dit **ce qui est vérifié, ce qui ne l'est pas, et ce qui est impossible**. Rien n'y est affirmé sans preuve ;
les tests se rejouent avec `scripts/smoke-matrix.sh`.

## 1. Résumé

| | |
| --- | --- |
| Plateformes visées | Bukkit, Spigot, Paper (et dérivés), Folia |
| Versions du jeu visées | 1.8 → dernière (numérotation « 26.x » comprise) |
| Jar | Un seul, Java 8, compilé contre l'API Spigot 1.8.8 |
| Java à l'exécution | Celui qu'exige votre version du jeu (voir §4) |
| Hors périmètre | Forge, NeoForge, Fabric, Sponge, BungeeCord, Velocity (§5) |

## 2. Ce qui a été vérifié sur de vrais serveurs

Date : 25 septembre 2026. Matériel : machine partagée, 2 cœurs, serveurs de test lancés un par un, sur `127.0.0.1`, mode hors ligne.

| Serveur | Java | Résultat | Joueurs simulés | Durée |
| --- | --- | --- | --- | --- |
| paper 1.8.8 | openjdk version "1.8.0_504" | réussi | 38/38 | 83 s |
| paper 1.12.2 | openjdk version "1.8.0_504" | réussi | 38/38 | 83 s |
| paper 1.16.5 | openjdk version "1.8.0_504" | réussi | 38/38 | 91 s |
| paper 1.18.2 | openjdk version "17.0.20.1" 2026-08-18 | réussi | 38/38 | 95 s |
| paper 1.20.4 | openjdk version "17.0.20.1" 2026-08-18 | réussi | 38/38 | 98 s |
| paper 1.21.11 | openjdk version "21.0.12.1" 2026-08-18 LTS | réussi | 38/38 | 106 s |
| paper 26.1.2 | openjdk version "25.0.4.1" 2026-08-18 LTS | **ÉCHEC** | 37/38 | 106 s |
| folia 1.21.11 | openjdk version "21.0.12.1" 2026-08-18 LTS | réussi | 38/38 | 105 s |
| folia 26.1.2 | openjdk version "25.0.4.1" 2026-08-18 LTS | réussi | 38/38 | 105 s |

Lecture : une ligne « réussi » veut dire que le test de fumée est passé **et** que les 38 vérifications des joueurs simulés ont réussi.
Cette table est celle de la **dernière** matrice complète, après le renommage en EGC-plugins. Une ligne en échec y figure telle quelle :
sur Paper 26.1.2, une étape (`/tpa` puis `/tpaccept` : l'arrivée du joueur près de l'autre) a dépassé son délai une fois sur cette
matrice ; deux reprises immédiates ont réussi 38/38. Je l'attribue au chargement du chunk d'arrivée sur une machine chargée (le délai du test
a été élargi ensuite), mais je ne l'ai **pas démontré**. Auparavant, les 9 serveurs avaient tous réussi 38/38 sur la version précédente du plugin.

Ce que couvre chaque essai :

- **Test de fumée** : le serveur démarre, le plugin se charge, `/uc selftest` passe (17 vérifications : versions, matériaux avant/après la
  1.13, enchantements, sons, objets, messages, couleurs, fichiers de données, commandes, planificateur), `/uc reload` puis un second
  auto-test, une vingtaine de commandes lancées **depuis la console**, arrêt propre, et **aucune exception du plugin dans le journal**.
- **Joueurs simulés** (colonne du même nom) : deux clients (mineflayer) se connectent et essaient, en vrai : message de bienvenue, solde
  de départ, maisons (limite, attente, annulation), kits (objets reçus, temps de recharge), chat (majuscules, répétition, spam),
  `/tpa` `/tpahere` `/tpaccept` `/tpdeny` `/tpcancel`, `/pay`, `/eco` depuis la console, permissions, `/heal` `/fly` `/gamemode` `/speed`
  `/ping`, spawn, `/back`, points de passage, titre et barre d'action **reçus par le joueur**.

## 3. Ce qui n'a pas été vérifié

| Sujet | Pourquoi |
| --- | --- |
| Spigot et CraftBukkit « purs » | Non testés : seuls les jars Paper et Folia sont téléchargeables de façon fiable et automatisée. Paper est un dérivé de Spigot, mais un défaut propre à Spigot passerait inaperçu. (Paper 1.8.8, ancien « PaperSpigot », se déclare comme Paper.) |
| Purpur, Pufferfish, Leaf, autres dérivés | Non testés. Ils suivent l'API de Paper : attendus compatibles, sans preuve. |
| Versions du jeu hors de la liste ci-dessus (1.9 à 1.11, 1.13 à 1.15, 1.17, 1.19, 1.20.1…) | Non testées. La compatibilité y est attendue (même code, même API), sans preuve. |
| Serveurs en mode en ligne (chat signé) | Les tests tournent en mode hors ligne. |
| Folia avec plusieurs joueurs dans des régions différentes | Les deux joueurs simulés sont proches l'un de l'autre au départ ; les scénarios inter-régions ne sont pas provoqués. |
| Windows, macOS | Non testés (le développement est fait sous Linux). Le code est du Java pur et n'utilise pas de chemin propre à un système. |
| Charge (dizaines de joueurs) | Non testée. |
| Folia et `max-players` | Un serveur Folia réglé sur 2 places a refusé un 2e joueur (« The server is full! ») alors qu'un seul était connecté ; la cause n'a pas été élucidée (les essais utilisent 4 places). Non attribué au plugin, non écarté non plus. |

## 4. Java requis à l'exécution

Le jar est du Java 8. Le serveur, lui, impose sa propre version de Java :

| Version du jeu | Java |
| --- | --- |
| 1.8 à 1.16 | 8 (ou plus, selon le serveur) |
| 1.17 | 16 ou plus |
| 1.18 à 1.20.4 | 17 ou plus |
| 1.20.5 à 1.21.x | 21 ou plus |
| 26.x | 25 (celle utilisée pour les essais) |

## 5. Ce qui est impossible, et pourquoi

| Cible | Raison |
| --- | --- |
| Forge, NeoForge, Fabric | Ces chargeurs n'exposent pas l'API Bukkit : le plugin est du code Bukkit. Un port demande un autre module avec l'API du chargeur (`libs/common` est réutilisable tel quel). Les « hybrides » (Mohist, Arclight…) mélangent les deux mondes : non visés, non testés. |
| Sponge, BungeeCord, Velocity | Autres API. BungeeCord et Velocity sont des proxys : ils n'ont ni monde, ni joueurs « en jeu ». |
| 1.7.10 et avant | `Bukkit.getOnlinePlayers()` y renvoie un tableau ; le jar compilé contre 1.8.8 planterait à l'appel. |
| Un seul appel direct à certaines API récentes | Le jar est compilé contre 1.8.8 : les classes qui n'existent pas là (planificateur de Folia, `NamespacedKey`…) ne peuvent être utilisées que par réflexion. |

## 6. Les pièges de compatibilité traités

Le jar compile contre l'API 1.8.8 et doit tourner jusqu'à la version la plus récente. Certaines API changent de forme, et **l'erreur
n'apparaît qu'à l'exécution**. Voici comment chacune est traitée :

| Changement | Effet si appelé directement | Traitement |
| --- | --- | --- |
| Noms de matériaux (1.13) : `WOOD_SWORD` → `WOODEN_SWORD`… | Objet introuvable | `Materials` : essaie l'ancien et le nouveau nom |
| `Enchantment` devient une interface (1.21) | `IncompatibleClassChangeError` | `Enchants` : réflexion, anciens et nouveaux noms |
| `Sound` devient une interface (1.21.x) | Erreur d'exécution | `Sounds` : réflexion, noms d'avant et d'après la 1.9 |
| `InventoryView` devient une interface (1.21) | `IncompatibleClassChangeError` | **Non utilisé** (pas de menus à inventaire) |
| Folia : `teleport`, planificateurs classiques interdits | `UnsupportedOperationException` | `Scheduler` (planificateur régionalisé par réflexion), `Teleports.teleportAsync` |
| `Player.sendTitle` absent avant la 1.11 | Erreur | `PlayerCompat` : API si elle existe, sinon paquets historiques (`libs/nms`), sinon chat |
| Barre d'action absente de l'API avant la 1.9 | Erreur | idem |
| `Player.getPing` absent avant la 1.16/1.17 | Erreur | idem (champ interne `ping`) |
| Couleurs hexadécimales (1.16) | Codes affichés en clair | `ColorCodes` : couleur classique la plus proche avant la 1.16 |
| Chat asynchrone, signé (1.19) | — | Filtre en calcul pur ; le texte publié peut différer de ce que voit un client de test (voir ROADMAP) |
| `PlayerTeleportEvent` non émis par `teleportAsync` sur Folia | `/back` ne retrouvait rien | Le service de téléportation enregistre lui-même le point de départ (constaté avec les joueurs simulés) |
| `PlayerTeleportEvent` de `teleportAsync` sur Paper 1.16.5 annonce l'arrivée comme départ | `/back` ne bougeait pas | Idem : pour nos téléportations le service fait foi, l'événement ne complète que la console (`COMMAND`, `UNKNOWN`) |
