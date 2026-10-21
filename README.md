# KoriaFactions

Plugin de factions complet pour **KoriaMC** — Spigot / PaperSpigot **1.8.8**, Java 8.
Tout est configurable : règles, textes, commandes, permissions, menus, améliorations, chat.

## Compiler

```
mvn clean package
```

Le jar est produit dans `target/KoriaFactions-1.0.0.jar`. Dépendances optionnelles :
**Vault** (+ un plugin d'économie) et **PlaceholderAPI**.

## Configuration

La configuration tient en **5 fichiers JSON** avec des noms d'options en français :

| Fichier | Contenu |
|---|---|
| `config.json` | stockage, commande `/f` et chaque sous-commande (activation, alias, délai, permission), économie, chat (grades, canaux), classements, journal, annonces, sons |
| `factions.json` | règles des factions, rôles, permissions de faction (rôles, alliés, par joueur), flags, alliances, puissance, PvP, home, warps, téléportations, coffre, améliorations, inactivité |
| `territoire.json` | claims et overclaim, zones (wilderness, SafeZone, WarZone), protection, explosions, blocs et entités protégés, commandes bloquées, notifications, carte, seechunk |
| `messages.json` | tous les textes, dont l'aide des commandes (préfixes `[actionbar]` et `[title]`, `""` = message désactivé) |
| `menus.json` | tous les menus, éditeur `/f config` compris : titres, tailles, cases, objets, matériaux, brillance et effet de chaque type de clic (`COMMANDE:`, `CONSOLE:`, `MENU:`, `MESSAGE:`, `FERMER`, `ACTUALISER`, pages, `BASCULER`, `ACHETER`...). Rien n'est imposé par le code. |

**Dans le jeu** : `/f config` (permission `koriafactions.admin.config`, OP par défaut) ouvre un éditeur qui
parcourt ces fichiers. Clic pour activer / désactiver ou augmenter / diminuer, touche Q pour saisir une valeur au
clavier, Ctrl + Q pour revenir à la valeur par défaut. Chaque option affiche son explication, et la modification est
enregistrée puis appliquée immédiatement.

**Sans le jeu** : le JSON n'a pas de commentaires, l'explication de chaque option est dans `LISEZMOI.txt`,
régénéré dans le dossier du plugin à chaque démarrage. Une option absente prend la valeur par défaut du jar.

Les matériaux acceptent le nom 1.8.8 (`DIAMOND_BLOCK`), l'ID (`57`) ou `ID:data` (`35:14`),
ce qui permet de cibler les blocs moddés KoriaMC (conteneurs, blocs protégés, etc.).

`/f reload` recharge tout, sauf le type de stockage et le nom de la commande principale (redémarrage nécessaire).
Les erreurs de syntaxe JSON (avec la ligne) et les améliorations invalides sont affichées dans le chat de l'auteur
du `/f reload` ; un fichier invalide garde sa version précédente jusqu'à correction.

## Fonctionnalités

- **Factions** : création, dissolution (confirmation), renommage (délai), description, message du jour,
  invitations cliquables avec expiration, faction ouverte, bannissements, titres, journal (`/f logs`).
- **Rôles** : Chef, Co-Chef, Officier, Membre, Recrue ; limites par rôle ; permissions éditables en jeu
  (`/f perms`) pour 36 actions, par rôle, pour les alliés et par joueur (`/f perms <joueur>`).
- **Puissance** : par joueur (maximum selon permission), régénération en ligne et hors ligne,
  perte à la mort avec multiplicateur par territoire, gain au kill, plafond de faction, alerte « raidable ».
- **Territoire** : claim simple, en rayon, automatique ou par clic sur `/f map` ; claims connectés,
  zone tampon, distance du spawn / des zones ; claims gratuits ; overclaim configurable (alliés compris) ;
  mondes autorisés ; accès par chunk (`/f access`) ; SafeZone / WarZone admin ; `/f seechunk`.
- **Relations** : toutes les factions sont ennemies par défaut ; alliances sur demande réciproque, avec limite et délai.
- **Protection** : blocs, conteneurs, portes, boutons, leviers, plaques, objets, entités, spawners,
  pistons, liquides, feu, explosions (en ligne / hors ligne, dégâts de blocs uniquement pour les canons),
  endermen, ender pearls, commandes bloquées, « pain build ».
- **PvP** : règles par relation, factions pacifiques, sans-faction, flags de territoire, potions nocives.
- **Home et warps** (mots de passe), téléportation instantanée depuis son territoire si aucun ennemi n'est
  proche, sinon délai d'attente annulé au mouvement / aux dégâts ; délais de réutilisation.
- **Chat** : tag de faction, couleur du pseudo selon le grade du serveur, canaux faction / alliés / modération
  (`/f c f`, `/f c a`, `/f c p`), préfixes rapides, espionnage admin.
- **Économie des joueurs** (Vault) : coût des commandes, prix des améliorations et récompenses sont pris sur le compte
  du joueur. Il n'existe aucun argent de faction. Économie activée sans Vault : les actions payantes sont refusées.
- **Améliorations** achetables dans un menu, dont la réduction des dégâts subis dans son territoire.
- **Coffre de faction** débloqué par amélioration (1 à 6 lignes), avec liste noire d'objets.
- **Classements** `/f top` : puissance, claims, membres.
- **Inactivité** : expulsion, transfert du commandement, dissolution, purge.
- **Stockage** JSON (copies de sécurité), SQLite ou MySQL, sauvegarde asynchrone différentielle.

## Commandes principales

`/f help` liste toutes les commandes disponibles pour le joueur (`/f help admin` pour l'administration).
Leurs alias, délais et permissions se règlent dans `config.json` (section `commandes`).

| Joueur | | Admin |
|---|---|---|
| `create`, `disband`, `rename`, `desc`, `motd` | `home`, `sethome`, `delhome` | `bypass`, `spy` |
| `show`, `list`, `top`, `power`, `quests` | `warp`, `setwarp`, `delwarp`, `warps` | `safezone`, `warzone` |
| `invite`, `deinvite`, `invites`, `join`, `leave` | `chat` (`c`), `announce`, `coords` | `setpower`, `addpower`, `setpoints`, `addpoints` |
| `kick`, `ban`, `unban`, `banlist` | `chest`, `upgrades`, `perms`, `flags`, `open` | `forcejoin`, `forcekick`, `setleader` |
| `promote`, `demote`, `leader`, `title` | `map`, `seechunk` | `setflag`, `setupgrade` |
| `claim`, `unclaim`, `unclaimall`, `autoclaim`, `autounclaim`, `access` | `toggle`, `logs` | `config`, `save`, `reload` |
| `ally`, `enemy`, `relations` | `menu`, `version` | |

## Permissions

Les nœuds des sous-commandes sont définis dans `config.json` (`koriafactions.command.<nom>` pour les
joueurs, donnés à tous par défaut ; `koriafactions.admin.<nom>` pour l'administration, donnés aux OP).

| Permission | Effet |
|---|---|
| `koriafactions.admin` | toutes les permissions d'administration (op par défaut) |
| `koriafactions.bypass.commands` | ignore les commandes bloquées par territoire |
| `koriafactions.bypass.cooldown` / `.warmup` / `.cost` | pas de délai / pas d'attente / gratuit |
| `koriafactions.zone.safezone.build` / `.warzone.build` | construire dans les zones |
| `koriafactions.chat.color` | couleurs dans les canaux de faction |
| `koriafactions.power.<grade>` | puissance max (`factions.json`, `puissance.max-par-grade`) |
| `koriafactions.rank.<grade>` | couleur du pseudo dans le chat (`config.json`, `chat.grades`) |

## PlaceholderAPI

- `%koriafactions_<variable>%` : `faction`, `role`, `role_prefix`, `title`, `power`, `max_power`,
  `faction_power`, `faction_max_power`, `faction_points`, `claims`, `max_claims`, `members`, `members_online`,
  `kills`, `deaths`, `kdr` (du joueur), `territory`, `raidable`, `chat_mode`...
- `%koriafactions_top_<catégorie>_<rang>_name%` et `..._value%` (ex. `top_power_1_name`) pour les hologrammes.
- `%rel_koriafactions_relation%` et `%rel_koriafactions_relation_color%`.

## API pour les autres plugins

```java
Faction f = KoriaFactionsAPI.getFaction(player);
Faction here = KoriaFactionsAPI.getFactionAt(player.getLocation());
boolean home = KoriaFactionsAPI.isInOwnTerritory(player);
```

Évènements Bukkit (annulables) dans `fr.koriamc.factions.api.event` :
`FactionCreateEvent`, `FactionDisbandEvent`, `FactionMembershipEvent`, `FactionRelationEvent`,
`LandClaimEvent`, `LandUnclaimEvent`, `PowerLossEvent`.
