# Récompenses Azuriom avec EGC-plugins

Azuriom (plugin **Vote**, boutique) exécute des **commandes console** sur le serveur de jeu, via AzLink. `{player}` est remplacé
par le pseudo du joueur. Toutes les commandes d'EGC-plugins acceptent la console.

## Récompense de vote

Dans l'administration du site : Vote → Récompenses → Commandes. Une ligne par commande :

```
eco give {player} 250
kit starter {player}
uc title {player} &6Merci !|&7Votre vote est compté
uc sound {player} LEVEL_UP
uc broadcast &e{player} &fa voté pour le serveur !
```

Les noms de sons et de matériaux fonctionnent quelle que soit la version du jeu (ancien ou nouveau nom).

## Kit livré par la boutique

Définissez le kit dans `config.yml` (section `kits:`), puis mettez comme commande d'un article : `kit vip {player}`.
Un kit donné par la console ignore la permission et le temps de recharge du joueur (c'est une livraison, pas une demande).

## Commandes utiles côté administration

| Commande | Effet |
| --- | --- |
| `eco give\|take\|set\|reset <joueur> [montant]` | Gère un solde (joueurs déjà venus, même hors ligne) |
| `kit <nom> <joueur>` | Donne un kit à un joueur en ligne |
| `uc broadcast <texte>` | Annonce (couleurs `&a`, `&#rrggbb`) |
| `uc title <joueur\|*> <titre>\|<sous-titre>` | Titre à l'écran |
| `uc actionbar <joueur\|*> <texte>` | Message au-dessus de la barre d'objets |

## À savoir

- Un joueur **hors ligne** ne reçoit pas de kit ni de titre : AzLink garde la commande en attente jusqu'à sa connexion
  **[à vérifier dans la configuration d'AzLink de votre serveur]**.
- Ne mettez jamais de jeton dans une commande de récompense.
