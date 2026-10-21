# KoriaFactions : documentation de configuration

Ce document décrit **tous les réglages** de KoriaFactions : où ils se trouvent, leur valeur par défaut et leur effet.
Il s'adresse aux administrateurs du serveur ; aucune connaissance en programmation n'est nécessaire.

## 1. Principes

### Fichiers

Dans `plugins/KoriaFactions/` :

| Fichier | Contenu |
|---|---|
| `config.json` | stockage, commande `/f` et chaque sous-commande, économie, chat, classements, journal, annonces, sons |
| `factions.json` | règles des factions, rôles, permissions, flags, alliances, puissance, PvP, home, warps, téléportations, coffre, améliorations, inactivité |
| `territoire.json` | claims et overclaim, zones (wilderness, SafeZone, WarZone), protection, explosions, blocs et entités protégés, notifications, carte, seechunk |
| `messages.json` | tous les textes envoyés aux joueurs |
| `menus.json` | tous les menus, y compris l'éditeur `/f config` |
| `LISEZMOI.txt` | explication de chaque option, **régénéré à chaque démarrage** (ne pas le modifier) |
| `data/` | données des factions et des joueurs (ne pas modifier serveur allumé) |

### Écrire du JSON

- Texte entre **guillemets doubles** : `"nom": "Chef"`. Nombres et `true` / `false` sans guillemets.
- Une **virgule** entre deux éléments, **jamais** après le dernier.
- Listes entre crochets : `["minage", "event"]`. Liste vide : `[]`.
- Le JSON n'accepte **pas de commentaires** : les explications sont dans ce document, dans `LISEZMOI.txt` et dans l'éditeur.
- En cas d'erreur, la console indique le fichier, la cause et la position, par exemple
  `Erreur dans factions.json : virgule manquante (ligne 42, colonne 5)`. Au démarrage, les valeurs par défaut sont
  alors utilisées ; lors d'un rechargement, l'ancienne version du fichier reste active jusqu'à correction.

### Valeurs par défaut

Une option **absente** du fichier prend la valeur par défaut du plugin (la console liste ces options au chargement).
On peut donc supprimer une option pour revenir à la valeur d'origine, ou supprimer un fichier entier pour le
régénérer au prochain démarrage.

Exception, les **sections libres** : leurs entrées ne sont pas complétées par les valeurs par défaut, on peut en
ajouter ou en supprimer librement. Ce sont `chat.grades`, `puissance.max-par-grade`,
`permissions.verrouillees`, `ameliorations.liste` (et les niveaux de chaque amélioration),
`protection.commandes-bloquees`, et dans les menus `principal.objets`, `sans-faction.objets`,
`permissions.objets` et `editeur.fichiers`.

### Appliquer les changements

- `/f reload` recharge tout ; les problèmes (erreur JSON, amélioration invalide) s'affichent dans le chat.
- Deux réglages demandent un **redémarrage** : `stockage.type` et le nom / les alias de la commande principale.

### Éditeur en jeu : `/f config`

Permission `koriafactions.admin.config` (OP par défaut). Choisir un fichier, puis naviguer dans les sections.
Chaque option affiche son explication et sa valeur.

| Type d'option | Clic |
|---|---|
| Oui / non | clic : basculer |
| Nombre | clic gauche / droit : + / − ; shift + clic : ×10 ; touche Q : saisir au clavier |
| Choix | clic gauche / droit : valeur suivante / précédente ; Q : saisir |
| Texte | clic : saisir dans le chat (`vide` pour une valeur vide, `annuler` pour renoncer) |
| Liste | clic gauche : ajouter ; clic droit : retirer la dernière ; shift + clic : remplacer toute la liste |
| Section libre | bouton émeraude : copier la dernière entrée sous un nouveau nom |
| Toutes | Ctrl + Q : revenir à la valeur par défaut (ou supprimer une entrée libre, à confirmer) |

Chaque modification est enregistrée dans le fichier et appliquée aussitôt. Les listes d'objets complexes (ex. niveaux
d'amélioration écrits en liste) ne sont modifiables que dans le fichier.

### Formats communs

| Format | Exemple | Règle |
|---|---|---|
| Couleurs | `&6Chef`, `&l&cALERTE` | codes `&0` à `&f`, `&l` gras, `&o` italique, `&n` souligné, `&m` barré, `&r` réinitialiser |
| Matériaux | `DIAMOND_BLOCK`, `57`, `35:14` | nom 1.8.8, ID ou ID:data (blocs moddés KoriaMC compris) |
| Sons | `LEVEL_UP`, `NOTE_PLING:1:2` | nom 1.8.8, puis volume et hauteur optionnels ; `""` = aucun son |
| Ticks | `20` | 20 ticks = 1 seconde |
| Mondes | `["minage"]` | noms des dossiers de monde |
| Territoires | `OWN`, `ALLY`, `ENEMY`, `WILDERNESS`, `SAFEZONE`, `WARZONE` | territoire vu par le joueur (OWN = le sien) |
| Rôles | `LEADER`, `COLEADER`, `OFFICER`, `MEMBER`, `RECRUIT` | du plus haut au plus bas |
| Expressions régulières | `^[a-zA-Z0-9_]+$` | caractères autorisés (noms de faction, de warp) |
| Variables | `{faction}`, `{player}` | remplacées dans les textes ; la liste dépend du message ou du menu |

## 2. `config.json`

### Options générales

| Option | Défaut | Description |
|---|---|---|
| `mondes-desactives` | `[]` | Mondes où le plugin est complètement ignoré (aucune protection, aucun claim). |
| `debug` | `false` | Affiche des informations supplémentaires dans la console. |

### `stockage`

Où sont enregistrées les factions et les joueurs. Changer le type nécessite un redémarrage.

| Option | Défaut | Description |
|---|---|---|
| `stockage.​type` | `"JSON"` | Type de stockage (redémarrage nécessaire).<br>*Valeurs :* `JSON`, `SQLITE`, `MYSQL` |
| `stockage.​sauvegarde-auto-minutes` | `5` | Sauvegarde automatique toutes les X minutes (0 = seulement à l'arrêt). |
| `stockage.​copies-de-securite` | `3` | Nombre de copies de sécurité des fichiers JSON conservées (0 = aucune). |
| `stockage.​json-indente` | `true` | Fichiers de données JSON lisibles (plus gros) ou compacts. |
| `stockage.​sqlite-fichier` | `"database.db"` | Nom du fichier SQLite dans le dossier du plugin. |
| `stockage.​mysql.​hote` | `"localhost"` | Adresse du serveur MySQL. |
| `stockage.​mysql.​port` | `3306` | Port du serveur MySQL. |
| `stockage.​mysql.​base` | `"koriafactions"` | Nom de la base de données. |
| `stockage.​mysql.​utilisateur` | `"root"` | Utilisateur MySQL. |
| `stockage.​mysql.​mot-de-passe` | `""` | Mot de passe MySQL. |
| `stockage.​mysql.​prefixe-tables` | `"kf_"` | Préfixe ajouté au nom des tables. |
| `stockage.​mysql.​parametres` | `"?useSSL=false&autoReconnect=true&useUnicode=true&characterEncoding=utf8"` | Paramètres ajoutés à l'adresse de connexion. |

### `commande`

Commande principale (/f). Changer le nom nécessite un redémarrage.

| Option | Défaut | Description |
|---|---|---|
| `commande.​nom` | `"f"` | Nom de la commande principale (redémarrage nécessaire). |
| `commande.​alias` | `["faction", "factions", "fac"]` | Autres noms de la commande principale (redémarrage nécessaire). |
| `commande.​description` | `"Commandes de faction"` | Description de la commande principale. |
| `commande.​sans-argument` | `"AIDE"` | Ce que fait la commande seule (/f).<br>*Valeurs :* `AIDE`, `MENU` |
| `commande.​aide-par-page` | `8` | Nombre de commandes par page dans /f help. |
| `commande.​aide-cliquable` | `true` | Lignes de /f help cliquables (écrit la commande dans le chat). |

### `commandes`

Chaque sous-commande : actif, alias, délai entre deux utilisations (secondes) et permission.

| Entrée | `actif` | `alias` | `delai` | `permission` | `commande` |
|---|---|---|---|---|---|
| `help` | `true` | `["aide", "?"]` | `0` | `"koriafactions.command.help"` | — |
| `menu` | `true` | `["gui", "m"]` | `0` | `"koriafactions.command.menu"` | — |
| `create` | `true` | `["creer", "make"]` | `3` | `"koriafactions.command.create"` | — |
| `disband` | `true` | `["dissoudre", "delete"]` | `0` | `"koriafactions.command.disband"` | — |
| `rename` | `true` | `["tag", "renommer"]` | `0` | `"koriafactions.command.rename"` | — |
| `desc` | `true` | `["description"]` | `0` | `"koriafactions.command.desc"` | — |
| `motd` | `true` | `[]` | `0` | `"koriafactions.command.motd"` | — |
| `show` | `true` | `["who", "info", "i"]` | `1` | `"koriafactions.command.show"` | — |
| `list` | `true` | `["liste", "ls"]` | `1` | `"koriafactions.command.list"` | — |
| `map` | `true` | `["carte"]` | `1` | `"koriafactions.command.map"` | — |
| `claim` | `true` | `["revendiquer", "cl"]` | `0` | `"koriafactions.command.claim"` | — |
| `claimat` | `true` | `[]` | `0` | `"koriafactions.command.claim"` | — |
| `unclaim` | `true` | `["uc", "declaim"]` | `0` | `"koriafactions.command.unclaim"` | — |
| `unclaimall` | `true` | `["uca", "declaimall"]` | `0` | `"koriafactions.command.unclaimall"` | — |
| `autoclaim` | `true` | `["aclaim"]` | `0` | `"koriafactions.command.autoclaim"` | — |
| `autounclaim` | `true` | `["aunclaim"]` | `0` | `"koriafactions.command.autounclaim"` | — |
| `invite` | `true` | `["inv", "inviter"]` | `0` | `"koriafactions.command.invite"` | — |
| `deinvite` | `true` | `["uninvite", "desinviter"]` | `0` | `"koriafactions.command.deinvite"` | — |
| `invites` | `true` | `["invitations"]` | `0` | `"koriafactions.command.invites"` | — |
| `join` | `true` | `["rejoindre"]` | `0` | `"koriafactions.command.join"` | — |
| `leave` | `true` | `["quitter"]` | `0` | `"koriafactions.command.leave"` | — |
| `kick` | `true` | `["expulser"]` | `0` | `"koriafactions.command.kick"` | — |
| `ban` | `true` | `["bannir"]` | `0` | `"koriafactions.command.ban"` | — |
| `unban` | `true` | `["debannir"]` | `0` | `"koriafactions.command.unban"` | — |
| `banlist` | `true` | `["bans"]` | `0` | `"koriafactions.command.banlist"` | — |
| `promote` | `true` | `["promouvoir", "up"]` | `0` | `"koriafactions.command.promote"` | — |
| `demote` | `true` | `["retrograder", "down"]` | `0` | `"koriafactions.command.demote"` | — |
| `leader` | `true` | `["chef", "owner"]` | `0` | `"koriafactions.command.leader"` | — |
| `title` | `true` | `["titre"]` | `0` | `"koriafactions.command.title"` | — |
| `ally` | `true` | `["allie", "allier"]` | `3` | `"koriafactions.command.relation"` | — |
| `enemy` | `true` | `["ennemi", "unally"]` | `3` | `"koriafactions.command.relation"` | — |
| `relations` | `true` | `["rel"]` | `0` | `"koriafactions.command.relations"` | — |
| `home` | `true` | `[]` | `0` | `"koriafactions.command.home"` | — |
| `sethome` | `true` | `[]` | `0` | `"koriafactions.command.sethome"` | — |
| `delhome` | `true` | `[]` | `0` | `"koriafactions.command.delhome"` | — |
| `warp` | `true` | `["w"]` | `0` | `"koriafactions.command.warp"` | — |
| `setwarp` | `true` | `["sw", "createwarp"]` | `0` | `"koriafactions.command.setwarp"` | — |
| `delwarp` | `true` | `["dw", "deletewarp"]` | `0` | `"koriafactions.command.delwarp"` | — |
| `warps` | `true` | `[]` | `0` | `"koriafactions.command.warp"` | — |
| `chat` | `true` | `["c", "ch", "canal"]` | `0` | `"koriafactions.command.chat"` | — |
| `chest` | `true` | `["coffre", "pv"]` | `0` | `"koriafactions.command.chest"` | — |
| `seechunk` | `true` | `["sc", "border"]` | `0` | `"koriafactions.command.seechunk"` | — |
| `top` | `true` | `["classement"]` | `2` | `"koriafactions.command.top"` | — |
| `quests` | `true` | `["quetes", "quete", "q"]` | `0` | `"koriafactions.command.quests"` | `"quetes faction"` |
| `power` | `true` | `["pow", "puissance", "p"]` | `0` | `"koriafactions.command.power"` | — |
| `perms` | `true` | `["permissions", "perm"]` | `0` | `"koriafactions.command.perms"` | — |
| `flags` | `true` | `["flag", "settings"]` | `0` | `"koriafactions.command.flags"` | — |
| `open` | `true` | `["ouvrir"]` | `0` | `"koriafactions.command.open"` | — |
| `access` | `true` | `["acces"]` | `0` | `"koriafactions.command.access"` | — |
| `upgrades` | `true` | `["upgrade", "ameliorations"]` | `0` | `"koriafactions.command.upgrades"` | — |
| `logs` | `true` | `["journal", "log"]` | `0` | `"koriafactions.command.logs"` | — |
| `announce` | `true` | `["annonce"]` | `30` | `"koriafactions.command.announce"` | — |
| `coords` | `true` | `["position", "pos"]` | `5` | `"koriafactions.command.coords"` | — |
| `toggle` | `true` | `["basculer"]` | `0` | `"koriafactions.command.toggle"` | — |
| `version` | `true` | `["ver", "about"]` | `0` | `"koriafactions.command.version"` | — |
| `bypass` | `true` | `["admin"]` | `0` | `"koriafactions.admin.bypass"` | — |
| `spy` | `true` | `["chatspy"]` | `0` | `"koriafactions.admin.spy"` | — |
| `safezone` | `true` | `["safe", "sz"]` | `0` | `"koriafactions.admin.safezone"` | — |
| `warzone` | `true` | `["war", "wz"]` | `0` | `"koriafactions.admin.warzone"` | — |
| `setpower` | `true` | `[]` | `0` | `"koriafactions.admin.power"` | — |
| `addpower` | `true` | `[]` | `0` | `"koriafactions.admin.power"` | — |
| `setpoints` | `true` | `[]` | `0` | `"koriafactions.admin.points"` | — |
| `addpoints` | `true` | `[]` | `0` | `"koriafactions.admin.points"` | — |
| `forcejoin` | `true` | `["fjoin"]` | `0` | `"koriafactions.admin.forcejoin"` | — |
| `forcekick` | `true` | `["fkick"]` | `0` | `"koriafactions.admin.forcekick"` | — |
| `setleader` | `true` | `[]` | `0` | `"koriafactions.admin.setleader"` | — |
| `setflag` | `true` | `[]` | `0` | `"koriafactions.admin.setflag"` | — |
| `setupgrade` | `true` | `[]` | `0` | `"koriafactions.admin.setupgrade"` | — |
| `config` | `true` | `["configuration", "cfg"]` | `0` | `"koriafactions.admin.config"` | — |
| `save` | `true` | `["sauvegarder"]` | `0` | `"koriafactions.admin.save"` | — |
| `reload` | `true` | `["rl", "recharger"]` | `0` | `"koriafactions.admin.reload"` | — |

- `actif` : false pour désactiver complètement la commande.
- `alias` : Autres noms de la commande.
- `delai` : Secondes à attendre entre deux utilisations (0 = aucun).
- `permission` : Permission requise. Donnée à tous par défaut, aux OP pour les commandes d'administration.

### `economie`

Économie des joueurs via Vault (il n'existe aucun argent de faction) : coûts des commandes, prix des améliorations et récompenses.

| Option | Défaut | Description |
|---|---|---|
| `economie.​actif` | `true` | true : les coûts sont payés par le compte Vault du joueur (refusés si Vault est absent). false : tout est gratuit. |
| `economie.​format` | `"{amount} $"` | Affichage des montants ({amount} = montant). |
| `economie.​decimales` | `0` | Nombre de décimales affichées. |
| `economie.​couts.​create` | `5000` | Prix de cette commande (0 = gratuit). |
| `economie.​couts.​rename` | `2500` | Prix de cette commande (0 = gratuit). |
| `economie.​couts.​sethome` | `0` | Prix de cette commande (0 = gratuit). |
| `economie.​couts.​home` | `0` | Prix de cette commande (0 = gratuit). |
| `economie.​couts.​setwarp` | `500` | Prix de cette commande (0 = gratuit). |
| `economie.​couts.​warp` | `0` | Prix de cette commande (0 = gratuit). |
| `economie.​couts.​invite` | `0` | Prix de cette commande (0 = gratuit). |
| `economie.​couts.​join` | `0` | Prix de cette commande (0 = gratuit). |
| `economie.​couts.​leave` | `0` | Prix de cette commande (0 = gratuit). |
| `economie.​couts.​description` | `0` | Prix de cette commande (0 = gratuit). |
| `economie.​couts.​motd` | `0` | Prix de cette commande (0 = gratuit). |
| `economie.​couts.​title` | `0` | Prix de cette commande (0 = gratuit). |
| `economie.​couts.​relation-ally` | `0` | Prix de cette commande (0 = gratuit). |
| `economie.​couts.​relation-enemy` | `0` | Prix de cette commande (0 = gratuit). |
| `economie.​couts.​announce` | `0` | Prix de cette commande (0 = gratuit). |
| `economie.​couts.​map` | `0` | Prix de cette commande (0 = gratuit). |
| `economie.​couts.​show` | `0` | Prix de cette commande (0 = gratuit). |
| `economie.​couts.​list` | `0` | Prix de cette commande (0 = gratuit). |
| `economie.​recompense-kill` | `0` | Argent donné au tueur (0 = désactivé). |
| `economie.​penalite-mort` | `0` | Argent retiré à la victime (0 = désactivé). |

### `chat`

Tag de faction, couleur de grade et canaux privés.

| Option | Défaut | Description |
|---|---|---|
| `chat.​actif` | `true` | Active la gestion du chat par le plugin. |
| `chat.​priorite` | `"HIGHEST"` | Priorité de l'écouteur de chat.<br>*Valeurs :* `LOWEST`, `LOW`, `NORMAL`, `HIGH`, `HIGHEST` |
| `chat.​mode-public` | `"FORMAT"` | FORMAT : le plugin gère tout le chat public. PLACEHOLDER : insère le tag dans le format d'un autre plugin. NONE : rien.<br>*Valeurs :* `FORMAT`, `PLACEHOLDER`, `NONE` |
| `chat.​format-public` | `"{tag}{rank_prefix}{rank_color}{player}&7: &f{message}"` | Format du chat public (mode FORMAT). Variables : {tag} {player} {message} {rank} {rank_color} {rank_prefix}. |
| `chat.​placeholder` | `"{FACTION}"` | Texte remplacé par le tag dans le format d'un autre plugin (mode PLACEHOLDER). |
| `chat.​tag` | `"&7[{relation_color}{role_prefix}{faction}&7] "` | Tag de faction. Variables : {relation_color} {role_prefix} {faction} {title}. |
| `chat.​tag-sans-faction` | `""` | Tag des joueurs sans faction. |
| `chat.​couleur-tag-par-defaut` | `"&e"` | Couleur {relation_color} quand elle n'est pas calculée par destinataire. |
| `chat.​couleur-par-destinataire` | `false` | Couleur du tag selon la relation avec chaque joueur (un message par joueur). |
| `chat.​sans-grade.​nom` | `"Joueur"` | Nom affiché ({rank}). |
| `chat.​sans-grade.​couleur` | `"&7"` | Couleur du pseudo ({rank_color}). |
| `chat.​sans-grade.​prefixe` | `""` | Préfixe devant le pseudo ({rank_prefix}). |
| `chat.​noms-des-canaux.​PUBLIC` | `["p", "public", "global", "g"]` | Noms qui sélectionnent ce canal. |
| `chat.​noms-des-canaux.​FACTION` | `["f", "faction", "fac"]` | Noms qui sélectionnent ce canal. |
| `chat.​noms-des-canaux.​ALLY` | `["a", "ally", "allie", "allies"]` | Noms qui sélectionnent ce canal. |
| `chat.​noms-des-canaux.​MOD` | `["m", "mod", "officier"]` | Noms qui sélectionnent ce canal. |
| `chat.​role-min-canal-mod` | `"OFFICER"` | Rôle minimum pour le canal MOD.<br>*Valeurs :* `RECRUIT`, `MEMBER`, `OFFICER`, `COLEADER`, `LEADER` |
| `chat.​format-espion` | `"&8[Espion] &7{message}"` | Format des messages vus par les admins en /f spy. |
| `chat.​canaux-dans-la-console` | `true` | Recopier les canaux privés dans la console. |

#### `chat.grades`

Grades du serveur : le premier dont le joueur a la permission donne la couleur du pseudo. Ces permissions ne sont données à personne par défaut, pas même aux OP.

| Entrée | `permission` | `nom` | `couleur` | `prefixe` |
|---|---|---|---|---|
| `empereur` | `"koriafactions.rank.empereur"` | `"Empereur"` | `"&6"` | `""` |
| `conquerant` | `"koriafactions.rank.conquerant"` | `"Conquérant"` | `"&5"` | `""` |
| `eclaireur` | `"koriafactions.rank.eclaireur"` | `"Éclaireur"` | `"&b"` | `""` |

- `permission` : Permission qui donne ce grade.
- `nom` : Nom du grade ({rank}).
- `couleur` : Couleur du pseudo ({rank_color}).
- `prefixe` : Préfixe devant le pseudo ({rank_prefix}).

| Entrée | `FACTION` | `ALLY` | `MOD` | Description |
|---|---|---|---|---|
| `canaux` | `"&a[F] &7{role_prefix}{title}{rank_color}{player}&7: &a{message}"` | `"&d[A] &7[{relation_color}{faction}&7] {rank_color}{player}&7: &d{message}"` | `"&e[Mod] &7{role_prefix}{rank_color}{player}&7: &e{message}"` | Formats des canaux privés. Variables : {player} {message} {faction} {relation_color} {role_prefix} {title} {rank} {rank_color} {rank_prefix}. |
| `prefixes-rapides` | `""` | `""` | `""` | Préfixe pour écrire directement dans un canal (ex. "!"), vide = désactivé. |

### `classement`

Classements /f top.

| Option | Défaut | Description |
|---|---|---|
| `classement.​par-page` | `10` | Factions par page. |
| `classement.​cache-secondes` | `30` | Durée pendant laquelle un classement est gardé en mémoire. |
| `classement.​categories` | `["POWER", "CLAIMS", "MEMBERS", "POINTS"]` | Catégories disponibles : POWER, CLAIMS, MEMBERS, POINTS. |
| `classement.​par-defaut` | `"POWER"` | Catégorie de /f top sans argument.<br>*Valeurs :* `POWER`, `CLAIMS`, `MEMBERS`, `POINTS` |

### `journal`

Journal de faction (/f logs).

| Option | Défaut | Description |
|---|---|---|
| `journal.​actif` | `true` | Active le journal. |
| `journal.​entrees-max` | `150` | Nombre d'entrées conservées par faction. |
| `journal.​par-page` | `10` | Entrées par page. |
| `journal.​format-date` | `"dd/MM HH:mm"` | Format de la date (ex. dd/MM HH:mm). |

### `annonces`

Annonces de faction (/f announce).

| Option | Défaut | Description |
|---|---|---|
| `annonces.​longueur-max` | `120` | Longueur maximale d'une annonce. |
| `annonces.​en-titre` | `true` | Afficher aussi l'annonce en titre à l'écran. |

### `sons`

Sons joués (noms de sons 1.8.8, vide = aucun son).

| Option | Défaut | Description |
|---|---|---|
| `sons.​invitation-recue` | `"NOTE_PLING"` | Invitation reçue. |
| `sons.​claim` | `"ORB_PICKUP"` | Chunk revendiqué. |
| `sons.​unclaim` | `"ITEM_BREAK"` | Chunk abandonné. |
| `sons.​clic-menu` | `"CLICK"` | Clic dans un menu. |
| `sons.​amelioration` | `"LEVEL_UP"` | Amélioration achetée. |
| `sons.​raidable` | `"WITHER_SPAWN"` | La faction devient raidable. |


**Chat, trois modes publics.** `FORMAT` : KoriaFactions écrit toute la ligne avec `format-public`. `PLACEHOLDER` :
un autre plugin de chat garde son format et KoriaFactions remplace `{FACTION}` (option `placeholder`) par le tag.
`NONE` : le chat public n'est pas touché (les canaux privés fonctionnent toujours). Les grades sont testés **dans
l'ordre du fichier** : placer le plus haut en premier.

**Économie.** Il n'existe aucun argent de faction : tous les coûts sont payés par le compte Vault du joueur. Pour
changer un prix, modifier sa valeur dans `economie.couts` (0 = gratuit) : seules les commandes déjà listées
sont payantes, une entrée ajoutée n'aurait pas d'effet. La permission `koriafactions.bypass.cost` rend tout gratuit pour un joueur.

## 3. `factions.json`

### `factions`

Règles générales des factions.

| Option | Défaut | Description |
|---|---|---|
| `factions.​nom-longueur-min` | `3` | Longueur minimale du nom. |
| `factions.​nom-longueur-max` | `10` | Longueur maximale du nom. |
| `factions.​nom-caracteres` | `"^[a-zA-Z0-9_]+$"` | Expression régulière que le nom doit respecter. |
| `factions.​noms-interdits` | `["admin", "staff", "modo", "koriamc", "safezone", "warzone", "wilderness"]` | Noms interdits (sans tenir compte des majuscules). |
| `factions.​noms-interdits-si-contenus` | `false` | Interdit aussi les noms qui contiennent un mot interdit. |
| `factions.​delai-renommage-minutes` | `60` | Minutes entre deux renommages. |
| `factions.​description-longueur-max` | `60` | Longueur maximale de la description. |
| `factions.​message-du-jour.​actif` | `true` | Active le message du jour. |
| `factions.​message-du-jour.​longueur-max` | `120` | Longueur maximale. |
| `factions.​message-du-jour.​a-la-connexion` | `true` | Afficher le message à la connexion. |
| `factions.​message-du-jour.​delai-ticks` | `40` | Délai avant l'affichage à la connexion (20 ticks = 1 seconde). |
| `factions.​membres-max` | `15` | Membres maximum (0 = illimité), sans compter l'amélioration Membres. |
| `factions.​role-a-l-arrivee` | `"RECRUIT"` | Rôle donné en rejoignant une faction.<br>*Valeurs :* `RECRUIT`, `MEMBER`, `OFFICER` |
| `factions.​co-chefs-max` | `2` | Co-chefs maximum (0 = illimité). |
| `factions.​officiers-max` | `0` | Officiers maximum (0 = illimité). |
| `factions.​invitations.​expiration-minutes` | `10` | Durée de validité (0 = illimitée). |
| `factions.​invitations.​max-en-attente` | `20` | Invitations en attente maximum par faction. |
| `factions.​invitations.​cliquables` | `true` | Bouton [ACCEPTER] cliquable dans le message. |
| `factions.​delai-avant-rejoindre-minutes` | `0` | Minutes à attendre avant de rejoindre une faction après en avoir quitté une. |
| `factions.​dissoudre-si-vide` | `true` | Dissoudre une faction qui n'a plus de membre (sauf permanente). |
| `factions.​le-chef-peut-quitter` | `false` | true : le commandement passe au membre le plus gradé. false : le chef doit d'abord le transférer. |
| `factions.​quitter-interdit-si-raidable` | `false` | Impossible de quitter une faction raidable. |
| `factions.​quitter-interdit-en-territoire-ennemi` | `true` | Impossible de quitter la faction depuis un territoire ennemi. |
| `factions.​dissolution-interdite-si-raidable` | `true` | Impossible de dissoudre une faction raidable. |
| `factions.​dissolution-lache-le-coffre` | `true` | À la dissolution, le contenu du coffre tombe aux pieds du chef. |
| `factions.​annonces-globales.​creation` | `true` | Annoncer cet évènement à tout le serveur. |
| `factions.​annonces-globales.​dissolution` | `true` | Annoncer cet évènement à tout le serveur. |
| `factions.​annonces-globales.​renommage` | `true` | Annoncer cet évènement à tout le serveur. |
| `factions.​annonces-globales.​alliance` | `true` | Annoncer cet évènement à tout le serveur. |
| `factions.​titres.​longueur-max` | `16` | Longueur maximale d'un titre. |
| `factions.​titres.​couleurs` | `false` | Autoriser les couleurs dans les titres. |
| `factions.​confirmations.​secondes` | `15` | Temps pour confirmer. |
| `factions.​confirmations.​dissolution` | `true` | Demander une confirmation pour cette action. |
| `factions.​confirmations.​unclaim-tout` | `true` | Demander une confirmation pour cette action. |
| `factions.​confirmations.​quitter` | `true` | Demander une confirmation pour cette action. |
| `factions.​confirmations.​expulsion` | `false` | Demander une confirmation pour cette action. |
| `factions.​notifier-connexions` | `true` | Prévenir la faction quand un membre se connecte. |
| `factions.​notifier-deconnexions` | `true` | Prévenir la faction quand un membre se déconnecte. |
| `factions.​liste.​tri` | `"ONLINE"` | Tri de /f list.<br>*Valeurs :* `ONLINE`, `POWER`, `MEMBERS`, `CLAIMS`, `NAME` |
| `factions.​liste.​par-page` | `10` | Factions par page. |

### `roles`

Rôles, du plus haut au plus bas. Le chef a toujours toutes les permissions. COLEADER et RECRUIT peuvent être désactivés.

| Entrée | `nom` | `prefixe` | `couleur` | `actif` |
|---|---|---|---|---|
| `LEADER` | `"Chef"` | `"**"` | `"&c"` | — |
| `COLEADER` | `"Co-Chef"` | `"*+"` | `"&6"` | `true` |
| `OFFICER` | `"Officier"` | `"*"` | `"&e"` | — |
| `MEMBER` | `"Membre"` | `"+"` | `"&a"` | — |
| `RECRUIT` | `"Recrue"` | `"-"` | `"&7"` | `true` |

- `nom` : Nom affiché.
- `prefixe` : Préfixe devant le pseudo.
- `couleur` : Couleur du rôle.
- `actif` : false pour supprimer ce rôle.

### `permissions`

Permissions de faction (/f perms). Les ennemis n'ont jamais aucune permission dans votre territoire.

| Option | Défaut | Description |
|---|---|---|
| `permissions.​par-joueur` | `true` | Autoriser les permissions personnalisées par membre (/f perms <joueur>). |
| `permissions.​par-defaut.​COLEADER` | `["*"]` | Actions autorisées par défaut à ce rôle ("*" = toutes). |
| `permissions.​par-defaut.​OFFICER` | `["BUILD", "DESTROY", "CONTAINER", "DOOR", "BUTTON", "LEVER", "PRESSURE_PLATE", "USE_ITEM", …]` (30) | Actions autorisées par défaut à ce rôle ("*" = toutes). |
| `permissions.​par-defaut.​MEMBER` | `["BUILD", "DESTROY", "CONTAINER", "DOOR", "BUTTON", "LEVER", "PRESSURE_PLATE", "USE_ITEM", …]` (15) | Actions autorisées par défaut à ce rôle ("*" = toutes). |
| `permissions.​par-defaut.​RECRUIT` | `["BUILD", "DESTROY", "DOOR", "BUTTON", "LEVER", "PRESSURE_PLATE", "INTERACT", "HOME", …]` (9) | Actions autorisées par défaut à ce rôle ("*" = toutes). |
| `permissions.​par-defaut.​ALLY` | `["DOOR", "BUTTON", "LEVER", "PRESSURE_PLATE"]` | Actions autorisées par défaut à ce rôle ("*" = toutes). |

#### `permissions.actions`

Nom, description et icône de chaque action dans /f perms.

| Entrée | `nom` | `description` | `icone` |
|---|---|---|---|
| `BUILD` | `"Construire"` | `"Poser des blocs"` | `"BRICK"` |
| `DESTROY` | `"Détruire"` | `"Casser des blocs"` | `"IRON_PICKAXE"` |
| `CONTAINER` | `"Conteneurs"` | `"Ouvrir coffres, fours, entonnoirs..."` | `"CHEST"` |
| `DOOR` | `"Portes"` | `"Utiliser portes, trappes et barrières"` | `"WOOD_DOOR"` |
| `BUTTON` | `"Boutons"` | `"Utiliser les boutons"` | `"STONE_BUTTON"` |
| `LEVER` | `"Leviers"` | `"Utiliser les leviers"` | `"LEVER"` |
| `PRESSURE_PLATE` | `"Plaques de pression"` | `"Activer les plaques de pression"` | `"STONE_PLATE"` |
| `USE_ITEM` | `"Objets"` | `"Seaux, briquets, œufs de spawn..."` | `"BUCKET"` |
| `INTERACT` | `"Interactions"` | `"Enclumes, tables d'enchantement, répéteurs..."` | `"ANVIL"` |
| `ENTITY_INTERACT` | `"Entités"` | `"Cadres, porte-armures, wagonnets..."` | `"ITEM_FRAME"` |
| `SPAWNER` | `"Spawners"` | `"Poser et casser des spawners"` | `"MOB_SPAWNER"` |
| `HOME` | `"Home"` | `"Se téléporter au home de faction"` | `"BED"` |
| `WARP` | `"Warps"` | `"Utiliser les warps de faction"` | `"ENDER_PEARL"` |
| `INVITE` | `"Inviter"` | `"Inviter des joueurs"` | `"PAPER"` |
| `KICK` | `"Expulser"` | `"Expulser des membres de rang inférieur"` | `"IRON_BOOTS"` |
| `BAN` | `"Bannir"` | `"Bannir des joueurs de la faction"` | `"BARRIER"` |
| `CLAIM` | `"Claim"` | `"Revendiquer des chunks"` | `"GRASS"` |
| `UNCLAIM` | `"Unclaim"` | `"Abandonner des chunks"` | `"DIRT"` |
| `UNCLAIM_ALL` | `"Unclaim tout"` | `"Abandonner tout le territoire"` | `"LAVA_BUCKET"` |
| `PROMOTE` | `"Promouvoir"` | `"Promouvoir des membres"` | `"GOLD_INGOT"` |
| `DEMOTE` | `"Rétrograder"` | `"Rétrograder des membres"` | `"IRON_INGOT"` |
| `RELATION` | `"Relations"` | `"Proposer ou rompre des alliances"` | `"GOLD_SWORD"` |
| `SETHOME` | `"Définir le home"` | `"Définir le home de faction"` | `"BED"` |
| `SETWARP` | `"Créer des warps"` | `"Créer des warps"` | `"EYE_OF_ENDER"` |
| `DELWARP` | `"Supprimer des warps"` | `"Supprimer des warps"` | `"ENDER_PEARL"` |
| `CHEST` | `"Coffre"` | `"Ouvrir le coffre de faction"` | `"ENDER_CHEST"` |
| `DESCRIPTION` | `"Description"` | `"Modifier la description"` | `"BOOK"` |
| `MOTD` | `"Message du jour"` | `"Modifier le message du jour"` | `"BOOK_AND_QUILL"` |
| `TITLE` | `"Titres"` | `"Donner des titres aux membres"` | `"NAME_TAG"` |
| `FLAGS` | `"Flags"` | `"Modifier les flags de la faction"` | `"BANNER"` |
| `UPGRADE` | `"Améliorations"` | `"Acheter des améliorations"` | `"EXP_BOTTLE"` |
| `ACCESS` | `"Accès"` | `"Gérer les accès par chunk"` | `"TRIPWIRE_HOOK"` |
| `ANNOUNCE` | `"Annonces"` | `"Envoyer des annonces"` | `"SIGN"` |
| `LOGS` | `"Journal"` | `"Consulter le journal de faction"` | `"MAP"` |
| `RENAME` | `"Renommer"` | `"Renommer la faction"` | `"NAME_TAG"` |
| `PERMS` | `"Permissions"` | `"Modifier les permissions (rangs inférieurs)"` | `"COMMAND"` |

### `flags`

Flags de faction (/f flags) : valeur à la création et modifiable ou non par les joueurs.

| Entrée | `defaut` | `modifiable` | `nom` | `description` | `icone` |
|---|---|---|---|---|---|
| `OPEN` | `false` | `true` | `"Ouverte"` | `"Tout le monde peut rejoindre sans invitation"` | `"WOOD_DOOR"` |
| `PEACEFUL` | `false` | `false` | `"Pacifique"` | `"Ses membres ne peuvent ni frapper ni être frappés"` | `"RED_ROSE"` |
| `PERMANENT` | `false` | `false` | `"Permanente"` | `"N'est jamais dissoute, même sans membre"` | `"BEDROCK"` |
| `EXPLOSIONS` | `true` | `true` | `"Explosions"` | `"Autorise les explosions dans le territoire"` | `"TNT"` |
| `FRIENDLY_FIRE` | `false` | `true` | `"Tir allié"` | `"Les membres peuvent se blesser entre eux"` | `"BOW"` |
| `PVP` | `true` | `false` | `"PvP"` | `"Autorise le PvP dans le territoire"` | `"DIAMOND_SWORD"` |
| `MONSTERS` | `true` | `true` | `"Monstres"` | `"Les monstres apparaissent dans le territoire"` | `"ROTTEN_FLESH"` |
| `FIRE_SPREAD` | `false` | `true` | `"Propagation du feu"` | `"Le feu peut se propager"` | `"FLINT_AND_STEEL"` |
| `INFINITE_POWER` | `false` | `false` | `"Puissance infinie"` | `"La faction n'est jamais raidable"` | `"NETHER_STAR"` |

- `defaut` : Valeur à la création de la faction.
- `modifiable` : Modifiable par les joueurs (sinon seulement par un admin avec /f setflag).

### `relations`

Toutes les factions sont ennemies par défaut. /f ally propose une alliance, /f enemy la rompt.

| Option | Défaut | Description |
|---|---|---|
| `relations.​alliances` | `true` | Autoriser les alliances. |
| `relations.​allies-max` | `2` | Alliés maximum (0 = illimité). |
| `relations.​delai-changement-secondes` | `30` | Secondes entre deux changements de relation avec la même faction. |

#### `relations.affichage`

Nom et couleur de chaque relation.

| Entrée | `nom` | `couleur` |
|---|---|---|
| `MEMBER` | `"Membre"` | `"&a"` |
| `ALLY` | `"Allié"` | `"&d"` |
| `ENEMY` | `"Ennemi"` | `"&c"` |

### `puissance`

Puissance des joueurs : elle limite le nombre de claims et rend la faction raidable quand elle baisse.

| Option | Défaut | Description |
|---|---|---|
| `puissance.​depart` | `5` | Puissance d'un nouveau joueur. |
| `puissance.​min` | `-10` | Puissance minimale d'un joueur. |
| `puissance.​max` | `10` | Puissance maximale d'un joueur sans grade. |
| `puissance.​regeneration.​intervalle-secondes` | `60` | Fréquence du calcul. |
| `puissance.​regeneration.​par-minute-connecte` | `0.2` | Gain par minute pour un joueur connecté. |
| `puissance.​regeneration.​par-minute-deconnecte` | `0` | Gain par minute hors ligne (ajouté à la reconnexion). |
| `puissance.​regeneration.​mondes-exclus` | `[]` | Aucun regain dans ces mondes. |
| `puissance.​perte-a-la-mort` | `4` | Puissance perdue à la mort. |
| `puissance.​perte-seulement-pvp` | `false` | Seules les morts causées par un joueur font perdre de la puissance. |
| `puissance.​pacifiques-sans-perte` | `true` | Les membres d'une faction pacifique ne perdent rien. |
| `puissance.​mondes-sans-perte` | `[]` | Aucune perte dans ces mondes. |
| `puissance.​multiplicateurs-de-perte.​WILDERNESS` | `1` | Multiplicateur de la perte pour une mort dans ce type de territoire (0 = aucune perte). |
| `puissance.​multiplicateurs-de-perte.​SAFEZONE` | `0` | Multiplicateur de la perte pour une mort dans ce type de territoire (0 = aucune perte). |
| `puissance.​multiplicateurs-de-perte.​WARZONE` | `1` | Multiplicateur de la perte pour une mort dans ce type de territoire (0 = aucune perte). |
| `puissance.​multiplicateurs-de-perte.​OWN` | `1` | Multiplicateur de la perte pour une mort dans ce type de territoire (0 = aucune perte). |
| `puissance.​multiplicateurs-de-perte.​ALLY` | `1` | Multiplicateur de la perte pour une mort dans ce type de territoire (0 = aucune perte). |
| `puissance.​multiplicateurs-de-perte.​ENEMY` | `1` | Multiplicateur de la perte pour une mort dans ce type de territoire (0 = aucune perte). |
| `puissance.​gain-par-kill` | `0` | Puissance gagnée par le tueur. |
| `puissance.​faction-max` | `0` | Plafond de puissance d'une faction (0 = aucun). |
| `puissance.​compter-hors-ligne` | `true` | Compter la puissance des membres hors ligne. |
| `puissance.​alerte-raidable` | `true` | Prévenir la faction quand elle devient raidable. |

#### `puissance.max-par-grade`

Puissance maximale selon une permission (la plus haute est retenue). Permissions données à personne par défaut.

| Entrée | `permission` | `max` |
|---|---|---|
| `eclaireur` | `"koriafactions.power.eclaireur"` | `12` |
| `conquerant` | `"koriafactions.power.conquerant"` | `14` |
| `empereur` | `"koriafactions.power.empereur"` | `16` |

### `pvp`

Combat entre joueurs.

| Option | Défaut | Description |
|---|---|---|
| `pvp.​entre-membres` | `false` | Dégâts entre membres (le flag FRIENDLY_FIRE d'une faction l'autorise quand même). |
| `pvp.​entre-allies` | `false` | Dégâts entre alliés. |
| `pvp.​entre-ennemis` | `true` | Dégâts entre ennemis. |
| `pvp.​sans-faction-peut-attaquer` | `true` | Un joueur sans faction peut attaquer. |
| `pvp.​sans-faction-peut-etre-attaque` | `true` | Un joueur sans faction peut être attaqué. |
| `pvp.​pacifiques-sans-pvp` | `true` | Aucun PvP pour les membres d'une faction pacifique. |
| `pvp.​mondes-sans-pvp` | `[]` | Mondes sans PvP (message dédié). |
| `pvp.​anti-spam-message-ms` | `1500` | Délai minimum entre deux messages de refus (millisecondes). |
| `pvp.​stats-seulement-pvp` | `true` | Ne compter kills et morts que si le tueur est un joueur. |

### `home`

Home de faction (/f home).

| Option | Défaut | Description |
|---|---|---|
| `home.​actif` | `true` | Active le home. |
| `home.​dans-son-territoire` | `true` | /f sethome uniquement dans son territoire. |
| `home.​refus-si-ennemi-a-moins-de` | `32` | Refusé si un ennemi est à moins de X blocs (0 = jamais). |
| `home.​refus-depuis-territoire-ennemi` | `true` | Refusé depuis un territoire ennemi. |
| `home.​refus-autre-monde` | `false` | Refusé si le home est dans un autre monde. |
| `home.​mondes-interdits` | `[]` | Mondes où le home est interdit. |
| `home.​reapparaitre-au-home` | `false` | Réapparaître au home de faction après la mort. |

### `warps`

Warps de faction (/f warp).

| Option | Défaut | Description |
|---|---|---|
| `warps.​actif` | `true` | Active les warps. |
| `warps.​max` | `2` | Warps maximum, sans compter l'amélioration Warps. |
| `warps.​dans-son-territoire` | `true` | Warps uniquement dans son territoire. |
| `warps.​nom-longueur-max` | `16` | Longueur maximale du nom. |
| `warps.​nom-caracteres` | `"^[a-zA-Z0-9_-]+$"` | Expression régulière que le nom doit respecter. |
| `warps.​refus-si-ennemi-a-moins-de` | `32` | Refusé si un ennemi est à moins de X blocs (0 = jamais). |
| `warps.​refus-depuis-territoire-ennemi` | `true` | Refusé depuis un territoire ennemi. |
| `warps.​mots-de-passe` | `true` | Autoriser les warps protégés par mot de passe. |

### `teleportation`

Délais du home et des warps.

| Option | Défaut | Description |
|---|---|---|
| `teleportation.​instantanee-dans-son-territoire` | `true` | Téléportation immédiate depuis son territoire si aucun ennemi n'est proche. |
| `teleportation.​rayon-ennemi-instantanee` | `32` | Rayon (blocs) sans ennemi requis pour la téléportation immédiate. |
| `teleportation.​annuler-si-mouvement` | `true` | Annuler l'attente si le joueur bouge. |
| `teleportation.​annuler-si-degats` | `true` | Annuler l'attente si le joueur subit des dégâts. |
| `teleportation.​compte-a-rebours` | `"ACTIONBAR"` | Affichage du compte à rebours.<br>*Valeurs :* `CHAT`, `ACTIONBAR`, `TITLE`, `NONE` |
| `teleportation.​sons.​tic` | `"CLICK"` | Son à chaque seconde d'attente. |
| `teleportation.​sons.​reussite` | `"ENDERMAN_TELEPORT"` | Son à l'arrivée. |
| `teleportation.​sons.​annulation` | `"VILLAGER_NO"` | Son quand l'attente est annulée. |

| Entrée | `home` | `warp` | Description |
|---|---|---|---|
| `attente-secondes` | `5` | `5` | Secondes à attendre sans bouger avant la téléportation. |
| `delai-secondes` | `30` | `30` | Secondes entre deux téléportations. |

### `coffre`

Coffre de faction (/f chest).

| Option | Défaut | Description |
|---|---|---|
| `coffre.​actif` | `true` | Active le coffre. |
| `coffre.​lignes-de-base` | `0` | Lignes sans amélioration (0 à 6). À 0, le coffre se débloque avec l'amélioration Coffre. |
| `coffre.​titre` | `"&8Coffre &7» &6{faction}"` | Titre du coffre ({faction}). |
| `coffre.​objets-interdits` | `[]` | Objets interdits dans le coffre. |

### `ameliorations`

Améliorations de faction (/f upgrades), payées par le joueur.

| Option | Défaut | Description |
|---|---|---|
| `ameliorations.​actif` | `true` | Active les améliorations. |
| `ameliorations.​annoncer-achat` | `true` | Annoncer l'achat à toute la faction. |
| `ameliorations.​reduction-degats-seulement-pvp` | `true` | La réduction de dégâts (DAMAGE_REDUCTION) ne concerne que les dégâts infligés par des joueurs. |
| `ameliorations.​reduction-degats-max` | `50` | Réduction de dégâts maximale en %, toutes améliorations confondues. |

#### `ameliorations.liste`

Chaque amélioration. On peut en ajouter ou en supprimer.

Structure d'une entrée (exemple : `membres`) :

| Clé | Exemple | Description |
|---|---|---|
| `type` | `"MEMBERS"` | Effet de l'amélioration.<br>*Valeurs :* `MEMBERS`, `WARPS`, `CHEST`, `POWER`, `CROPS`, `EXP`, `SPAWNERS`, `DAMAGE_REDUCTION` |
| `case` | `10` | Emplacement dans le menu (0 à 53). |
| `icone` | `"SKULL_ITEM:3"` | Objet affiché. |
| `nom` | `"&aMembres supplémentaires"` | Nom affiché. |
| `description` | `["&7Augmente le nombre maximum", "&7de membres de la faction."]` | Lignes de description. |
| `format-valeur` | `"+{value} membres"` | Affichage de l'effet ({value}). |
| `niveaux.*.cout` | `50000` | Prix du niveau. |
| `niveaux.*.valeur` | `2` | Valeur de l'effet à ce niveau. |

Entrées par défaut :

| Entrée | `type` | `case` | `icone` | `nom` | `format-valeur` | `niveaux` |
|---|---|---|---|---|---|---|
| `membres` | `"MEMBERS"` | `10` | `"SKULL_ITEM:3"` | `"&aMembres supplémentaires"` | `"+{value} membres"` | 1 : 50 000 → 2<br>2 : 150 000 → 4<br>3 : 400 000 → 6 |
| `puissance` | `"POWER"` | `11` | `"BLAZE_POWDER"` | `"&cPuissance bonus"` | `"+{value} puissance"` | 1 : 75 000 → 5<br>2 : 200 000 → 10<br>3 : 500 000 → 20 |
| `coffre` | `"CHEST"` | `12` | `"CHEST"` | `"&6Coffre de faction"` | `"{value} ligne(s)"` | 1 : 25 000 → 1<br>2 : 60 000 → 2<br>3 : 120 000 → 3<br>4 : 250 000 → 4<br>5 : 450 000 → 5<br>6 : 750 000 → 6 |
| `warps` | `"WARPS"` | `13` | `"EYE_OF_ENDER"` | `"&5Warps supplémentaires"` | `"+{value} warps"` | 1 : 40 000 → 1<br>2 : 120 000 → 2<br>3 : 300 000 → 3 |
| `defense` | `"DAMAGE_REDUCTION"` | `14` | `"IRON_CHESTPLATE"` | `"&9Défense du territoire"` | `"-{value}% de dégâts"` | 1 : 100 000 → 5<br>2 : 250 000 → 10<br>3 : 500 000 → 15<br>4 : 1 000 000 → 20 |
| `cultures` | `"CROPS"` | `15` | `"WHEAT"` | `"&eCroissance des cultures"` | `"{value}% de chance"` | 1 : 50 000 → 10<br>2 : 150 000 → 20<br>3 : 350 000 → 35 |
| `experience` | `"EXP"` | `16` | `"EXP_BOTTLE"` | `"&aExpérience bonus"` | `"+{value}% d'expérience"` | 1 : 40 000 → 25<br>2 : 120 000 → 50<br>3 : 300 000 → 100 |
| `spawners` | `"SPAWNERS"` | `22` | `"MOB_SPAWNER"` | `"&dSpawners boostés"` | `"{value}% de chance"` | 1 : 100 000 → 10<br>2 : 300 000 → 20<br>3 : 750 000 → 30 |

### `inactivite`

Gestion des joueurs inactifs.

| Option | Défaut | Description |
|---|---|---|
| `inactivite.​actif` | `false` | Active la gestion de l'inactivité. |
| `inactivite.​verification-minutes` | `60` | Fréquence de la vérification. |
| `inactivite.​expulsion-apres-jours` | `30` | Expulser un membre inactif depuis X jours (0 = jamais). |
| `inactivite.​dissolution-apres-jours` | `45` | Dissoudre une faction dont tous les membres sont inactifs depuis X jours (0 = jamais). |
| `inactivite.​transfert-du-chef` | `true` | Si le chef est inactif, le commandement passe au membre actif le plus gradé. |
| `inactivite.​purge-joueurs-apres-jours` | `90` | Supprimer les données des joueurs sans faction inactifs depuis X jours (0 = jamais). |


**Lire les permissions.** Un sujet (`COLEADER`, `OFFICER`, `MEMBER`, `RECRUIT` ou `ALLY`) reçoit une liste
d'actions ; `"*"` les donne toutes. Le **chef a toujours tout**. Les factions changent ensuite leurs permissions avec
`/f perms`, sauf les actions verrouillées : `"verrouillees": { "ALLY": ["SPAWNER"], "RECRUIT": ["UNCLAIM"] }`.
Les alliés n'ont que les actions de territoire (`BUILD` à `SPAWNER`) ; les ennemis n'ont jamais rien.

**Puissance et raidable.** Une faction garde `puissance / claims.puissance-par-claim` chunks. Si elle a plus de
claims que sa puissance ne le permet, elle devient **raidable** : ses chunks peuvent être overclaim et les ennemis
obtiennent les actions de `protection.actions-si-raidable`. La puissance maximale d'un joueur dépend de
`puissance.max` ou de la plus haute permission de `puissance.max-par-grade` qu'il possède.

**Ajouter une amélioration.** Copier une entrée de `ameliorations.liste` sous un nouveau nom, choisir son `type`,
une `case` libre du menu et ses niveaux (`"1": { "cout": 50000, "valeur": 2 }`, dans l'ordre). Plusieurs
améliorations du même type s'additionnent.

## 4. `territoire.json`

### `claims`

Revendication de territoire.

| Option | Défaut | Description |
|---|---|---|
| `claims.​mondes-mode` | `"BLACKLIST"` | BLACKLIST : claims interdits dans la liste. WHITELIST : uniquement dans la liste.<br>*Valeurs :* `BLACKLIST`, `WHITELIST` |
| `claims.​mondes` | `["minage"]` | Liste de mondes utilisée par mondes-mode. |
| `claims.​puissance-par-claim` | `1` | Puissance nécessaire par chunk. |
| `claims.​max-par-faction` | `0` | Chunks maximum par faction (0 = illimité, seule la puissance limite). |
| `claims.​rayon-max` | `5` | Rayon maximum de /f claim <rayon>. |
| `claims.​connectes` | `true` | Les claims doivent se toucher (le premier peut être posé n'importe où). |
| `claims.​zone-tampon` | `1` | Chunks libres obligatoires entre deux factions (0 = aucun). |
| `claims.​distance-min-spawn` | `0` | Distance minimale (chunks) du spawn du monde (0 = désactivé). |
| `claims.​distance-min-safezone` | `0` | Distance minimale (chunks) d'une SafeZone (0 = désactivé). |
| `claims.​distance-min-warzone` | `0` | Distance minimale (chunks) d'une WarZone (0 = désactivé). |
| `claims.​refus-si-ennemi-a-moins-de` | `0` | Impossible de claim si un ennemi est à moins de X blocs (0 = désactivé). |
| `claims.​overclaim.​actif` | `true` | Active l'overclaim. |
| `claims.​overclaim.​cible-raidable` | `true` | La faction ciblée doit être raidable. |
| `claims.​overclaim.​bordure-seulement` | `true` | Seulement les chunks en bordure du territoire adverse. |
| `claims.​overclaim.​sans-connexion` | `true` | Autoriser un overclaim non relié à son propre territoire. |
| `claims.​overclaim.​prevenir-la-victime` | `true` | Prévenir la faction volée. |
| `claims.​overclaim.​annonce-globale` | `false` | Annoncer l'overclaim à tout le serveur. |
| `claims.​supprimer-home-hors-territoire` | `true` | Supprimer le home quand son chunk est perdu. |
| `claims.​supprimer-warps-hors-territoire` | `true` | Supprimer les warps quand leur chunk est perdu. |
| `claims.​acces-par-chunk` | `true` | Autoriser /f access (accès d'un joueur ou d'une faction à un chunk). |
| `claims.​acces-actions` | `["BUILD", "DESTROY", "CONTAINER", "DOOR", "BUTTON", "LEVER", "PRESSURE_PLATE", "USE_ITEM", …]` (11) | Actions données par un accès de chunk. |

### `zones`

Wilderness (zone libre), SafeZone et WarZone : nom, couleur et règles.

| Option | `wilderness` | `safezone` | `warzone` | Description |
|---|---|---|---|---|
| `nom` | `"Wilderness"` | `"SafeZone"` | `"WarZone"` | Nom affiché. |
| `description` | `"Zone libre"` | `"Zone protégée, le PvP est désactivé"` | `"Zone de combat, restez sur vos gardes !"` | Description affichée en entrant. |
| `couleur` | `"&2"` | `"&6"` | `"&4"` | Couleur de la zone. |
| `construire` | `true` | `false` | `false` | Les joueurs peuvent poser des blocs. |
| `detruire` | `true` | `false` | `false` | Les joueurs peuvent casser des blocs. |
| `utiliser` | `true` | `false` | `false` | Les joueurs peuvent utiliser coffres, portes, objets. |
| `pvp` | `true` | `false` | `true` | PvP autorisé. |
| `monstres` | `true` | `false` | `false` | Apparition naturelle des monstres. |
| `propagation-feu` | `true` | `false` | `false` | Le feu se propage. |
| `mondes-construction-interdite` | `[]` | — | — | Mondes où la construction est interdite dans la wilderness (ex. monde du spawn). |
| `toujours-utilisables` | — | `["ENDER_CHEST", "WORKBENCH", "WOODEN_DOOR", "STONE_BUTTON", "WOOD_BUTTON"]` | `["ENDER_CHEST", "WORKBENCH"]` | Blocs toujours utilisables dans la zone. |
| `degats-joueurs` | — | `false` | — | Les joueurs subissent des dégâts (chute, feu, monstres...). |
| `faim` | — | `false` | `true` | La faim diminue. |
| `bloquer-liquides` | — | `true` | `true` | L'eau et la lave ne peuvent pas entrer dans la zone. |
| `ender-pearls` | — | `true` | `true` | Ender pearls autorisées. |
| `bloquer-potions-negatives` | — | `true` | — | Retirer les effets négatifs des potions jetées. |
| `tir-allie` | — | — | `false` | Dégâts entre membres et alliés même dans cette zone. |
| `degats-de-chute` | — | — | `true` | Dégâts de chute. |

### `protection`

Protection des territoires de faction.

| Option | Défaut | Description |
|---|---|---|
| `protection.​degats-si-construction` | `0` | Dégâts infligés à qui tente de construire chez les autres (0 = désactivé). |
| `protection.​actions-si-raidable` | `[]` | Actions autorisées aux ennemis quand la faction est raidable (ex. BUILD, DESTROY, CONTAINER). |
| `protection.​ender-pearls-interdites` | `["ENEMY"]` | Territoires où les ender pearls sont interdites : OWN, ALLY, ENEMY, WILDERNESS, SAFEZONE, WARZONE. |
| `protection.​proteger-pistons` | `true` | Les pistons ne poussent pas de blocs à travers une frontière. |
| `protection.​bloquer-liquides` | `true` | L'eau et la lave ne peuvent pas entrer dans un territoire depuis l'extérieur. |
| `protection.​entites-sans-degats-de-blocs` | `["ENDERMAN", "SILVERFISH", "WITHER", "ZOMBIE"]` | Entités qui ne peuvent pas modifier les blocs d'un territoire. |
| `protection.​monstres-raisons-controlees` | `["NATURAL", "JOCKEY", "CHUNK_GEN", "VILLAGE_INVASION", "REINFORCEMENTS", "NETHER_PORTAL"]` | Raisons d'apparition concernées par les règles de monstres. |
| `protection.​commandes-bloquees.​ENEMY` | `["sethome", "home", "homes", "spawn", "tpa", "tpaccept", "tpahere", "back", …]` (9) | Commandes interdites dans ce type de territoire (sans le /). |
| `protection.​commandes-bloquees.​ALLY` | `[]` | Commandes interdites dans ce type de territoire (sans le /). |
| `protection.​commandes-bloquees.​OWN` | `[]` | Commandes interdites dans ce type de territoire (sans le /). |
| `protection.​commandes-bloquees.​WILDERNESS` | `[]` | Commandes interdites dans ce type de territoire (sans le /). |
| `protection.​commandes-bloquees.​SAFEZONE` | `[]` | Commandes interdites dans ce type de territoire (sans le /). |
| `protection.​commandes-bloquees.​WARZONE` | `["sethome"]` | Commandes interdites dans ce type de territoire (sans le /). |

### `explosions`

Explosions qui détruisent des blocs selon l'endroit (TNT, CREEPER, FIREBALL, WITHER, OTHER).

| Option | Défaut | Description |
|---|---|---|
| `explosions.​seulement-blocs` | `true` | true : seules les destructions de blocs sont annulées (canons TNT fonctionnels). false : l'explosion entière est annulée. |
| `explosions.​proteger-pacifiques` | `true` | Les factions pacifiques sont toujours protégées. |

| Entrée | `TNT` | `CREEPER` | `FIREBALL` | `WITHER` | `OTHER` | Description |
|---|---|---|---|---|---|---|
| `wilderness` | `true` | `true` | `true` | `true` | `true` | Zone libre. |
| `safezone` | `false` | `false` | `false` | `false` | `false` | SafeZone. |
| `warzone` | `false` | `false` | `false` | `false` | `false` | WarZone. |
| `faction-en-ligne` | `true` | `false` | `true` | `true` | `true` | Territoire d'une faction dont au moins un membre est connecté. |
| `faction-hors-ligne` | `false` | `false` | `false` | `false` | `false` | Territoire d'une faction dont aucun membre n'est connecté. |

### `blocs`

Catégories de blocs (nom, ID ou ID:data) utilisées par les permissions de territoire.

| Option | Défaut | Description |
|---|---|---|
| `blocs.​conteneurs` | `["CHEST", "TRAPPED_CHEST", "FURNACE", "BURNING_FURNACE", "DISPENSER", "DROPPER", "HOPPER", "BREWING_STAND", …]` (10) | Permission CONTAINER. |
| `blocs.​portes` | `["WOODEN_DOOR", "SPRUCE_DOOR", "BIRCH_DOOR", "JUNGLE_DOOR", "ACACIA_DOOR", "DARK_OAK_DOOR", "TRAP_DOOR", "FENCE_GATE", …]` (13) | Permission DOOR. |
| `blocs.​boutons` | `["STONE_BUTTON", "WOOD_BUTTON"]` | Permission BUTTON. |
| `blocs.​leviers` | `["LEVER"]` | Permission LEVER. |
| `blocs.​plaques` | `["STONE_PLATE", "WOOD_PLATE", "GOLD_PLATE", "IRON_PLATE", "TRIPWIRE"]` | Permission PRESSURE_PLATE. |
| `blocs.​interactifs` | `["ANVIL", "ENCHANTMENT_TABLE", "DIODE_BLOCK_OFF", "DIODE_BLOCK_ON", "REDSTONE_COMPARATOR_OFF", "REDSTONE_COMPARATOR_ON", "NOTE_BLOCK", "CAULDRON", …]` (17) | Permission INTERACT. |
| `blocs.​toujours-autorises` | `["ENDER_CHEST", "WORKBENCH"]` | Toujours utilisables, quel que soit le territoire. |
| `blocs.​spawners` | `["MOB_SPAWNER"]` | Permission SPAWNER. |
| `blocs.​objets-restreints` | `["FLINT_AND_STEEL", "FIREBALL", "BUCKET", "WATER_BUCKET", "LAVA_BUCKET", "ARMOR_STAND", "MINECART", "STORAGE_MINECART", …]` (16) | Objets dont l'utilisation sur un bloc demande la permission USE_ITEM. |

### `entites`

Entités protégées dans les territoires.

| Option | Défaut | Description |
|---|---|---|
| `entites.​clic-droit-protege` | `["ITEM_FRAME", "ARMOR_STAND", "MINECART_CHEST", "MINECART_HOPPER", "MINECART_FURNACE", "LEASH_HITCH", "HORSE"]` | Clic droit : permission ENTITY_INTERACT. |
| `entites.​degats-proteges` | `["ITEM_FRAME", "ARMOR_STAND", "PAINTING", "MINECART_CHEST", "MINECART_HOPPER"]` | Dégâts : permission DESTROY. |
| `entites.​animaux-proteges` | `[]` | Animaux protégés contre les joueurs non autorisés. |

### `notifications`

Message en entrant dans un territoire.

| Option | Défaut | Description |
|---|---|---|
| `notifications.​actif` | `true` | Active les notifications. |
| `notifications.​affichage` | `["TITLE"]` | Types d'affichage combinables : TITLE, ACTIONBAR, CHAT. |
| `notifications.​titre-apparition` | `5` | Durée d'apparition du titre (ticks). |
| `notifications.​titre-duree` | `30` | Durée d'affichage du titre (ticks). |
| `notifications.​titre-disparition` | `10` | Durée de disparition du titre (ticks). |
| `notifications.​actives-par-defaut` | `true` | Activées pour les nouveaux joueurs (/f toggle notifications). |

### `carte`

Carte des territoires (/f map).

| Option | Défaut | Description |
|---|---|---|
| `carte.​largeur` | `39` | Largeur en chunks (impaire de préférence). |
| `carte.​hauteur` | `9` | Hauteur en chunks (impaire de préférence). |
| `carte.​symboles` | `"\\/#?$%=&^ABCDEFGHJKLMNOPQRSTUVWXYZ1234567890abcdeghjmnopqrsuvwxyz"` | Symboles attribués aux factions. |
| `carte.​symbole-joueur` | `"+"` | Symbole de la position du joueur. |
| `carte.​symbole-wilderness` | `"-"` | Symbole d'un chunk libre. |
| `carte.​symbole-safezone` | `"+"` | Symbole d'un chunk de SafeZone. |
| `carte.​symbole-warzone` | `"+"` | Symbole d'un chunk de WarZone. |
| `carte.​couleur-joueur` | `"&e"` | Couleur du symbole du joueur. |
| `carte.​boussole` | `true` | Afficher la boussole. |
| `carte.​infos-au-survol` | `true` | Nom de la faction au survol d'un chunk. |
| `carte.​claim-au-clic` | `true` | Revendiquer un chunk en cliquant dessus. |
| `carte.​legende-max` | `12` | Factions maximum dans la légende. |
| `carte.​distance-max-claim-au-clic` | `8` | Distance maximale (chunks) d'un claim par clic. |

### `seechunk`

Bordures de chunk en particules (/f seechunk).

| Option | Défaut | Description |
|---|---|---|
| `seechunk.​actif` | `true` | Active /f seechunk. |
| `seechunk.​particule` | `"FLAME"` | Particule 1.8.8.<br>*Valeurs :* `FLAME`, `HAPPY_VILLAGER`, `COLOURED_DUST`, `CLOUD`, `WITCH_MAGIC`, `FIREWORKS_SPARK` |
| `seechunk.​intervalle-ticks` | `20` | Fréquence d'affichage (ticks). |
| `seechunk.​hauteur-dessous` | `2` | Hauteurs affichées sous les yeux du joueur. |
| `seechunk.​hauteur-dessus` | `4` | Hauteurs affichées au-dessus des yeux du joueur. |
| `seechunk.​espacement` | `1` | Espacement entre deux particules (blocs). |


**Ordre de décision de la protection** pour une action dans un chunk : monde désactivé ou admin en bypass →
autorisé ; wilderness → règles de la zone ; SafeZone / WarZone → permission `koriafactions.zone.<zone>.build`, sinon
règles de la zone ; territoire de faction → accès du chunk (`/f access`), puis permissions de la faction, puis
`actions-si-raidable` si la faction est raidable et que le joueur est ennemi ; sinon refus.

**Explosions.** Pour chaque type de lieu, `true` autorise l'explosion à détruire des blocs. Une faction est
« en ligne » dès qu'un de ses membres est connecté. Le flag `EXPLOSIONS` d'une faction et
`proteger-pacifiques` peuvent encore l'interdire.

## 5. `messages.json`

Tous les textes, rangés par groupe. Les **clés** (noms à gauche) ne doivent pas être modifiées ; seuls les textes le sont.

| Forme | Effet |
|---|---|
| `"texte"` | une ligne dans le chat |
| `["ligne 1", "ligne 2"]` | plusieurs lignes |
| `"[actionbar]texte"` | barre d'action (au-dessus de la barre d'objets) |
| `"[title]Titre;;Sous-titre"` | titre à l'écran |
| `""` | message désactivé |

`{prefix}` insère le préfixe, `{label}` le nom de la commande (`f`). Une clé supprimée reprend son texte par défaut.
Les variables disponibles sont celles déjà utilisées dans le texte par défaut du même message.

| Groupe | Contenu | Clés | Variables utilisées |
|---|---|---|---|
| `prefix` | Préfixe inséré par {prefix} | 1 |  |
| `general` | Erreurs et réponses communes (permission, usage, joueur introuvable, rechargement...) | 24 | `{label}` `{command}` `{usage}` `{time}` `{player}` `{faction}` `{input}` `{seconds}` `{count}` `{problem}` `{action}` |
| `time` | Unités de durée (j, h, min, s) | 6 |  |
| `values` | Petits mots réutilisés (oui, non, aucun, illimité...) | 8 |  |
| `help` | /f help : en-tête, lignes, et usage + description de chaque commande | 148 | `{page}` `{pages}` `{label}` `{command}` `{usage}` `{description}` `{next}` |
| `create` | Création de faction | 7 | `{faction}` `{player}` `{min}` `{max}` |
| `disband` | Dissolution | 6 | `{faction}` `{player}` |
| `rename` | Renommage | 3 | `{faction}` `{old}` `{time}` |
| `description` | Description | 2 | `{description}` `{max}` |
| `motd` | Message du jour | 4 | `{max}` `{faction}` `{motd}` |
| `show` | /f show : lignes affichées et formats | 11 | `{relation_color}` `{faction}` `{raidable}` `{description}` `{age}` `{open}` `{leader}` `{power}` `{max_power}` `{points}` `{claims}` `{max_claims}` `{home}` `{allies_count}` `{allies}` `{online_count}` `{online}` `{offline_count}` `{offline}` `{role_color}` `{role_prefix}` `{player}` `{world}` `{x}` `{y}` `{z}` |
| `list` | /f list | 4 | `{page}` `{pages}` `{count}` `{relation_color}` `{faction}` `{online}` `{members}` `{claims}` `{power}` `{max_power}` |
| `map` | /f map : légende, boussole, survol | 10 | `{x}` `{z}` `{faction}` `{color}` `{symbol}` |
| `claim` | Claims et overclaims | 27 | `{x}` `{z}` `{faction}` `{count}` `{max}` `{attacker}` |
| `unclaim` | Abandon de chunks | 7 | `{x}` `{z}` `{count}` |
| `zone` | Claims de SafeZone / WarZone | 4 | `{count}` `{zone}` |
| `invite` | Invitations | 16 | `{player}` `{inviter}` `{faction}` `{label}` `{max}` `{name}` `{time}` |
| `join` | Adhésion | 6 | `{faction}` `{player}` `{max}` `{time}` |
| `leave` | Départ | 6 | `{faction}` `{player}` `{label}` |
| `kick` | Expulsion | 3 | `{player}` `{faction}` `{kicker}` |
| `ban` | Bannissements | 8 | `{player}` `{faction}` |
| `promote` | Promotions | 6 | `{player}` `{role}` `{max}` |
| `demote` | Rétrogradations | 4 | `{player}` `{role}` |
| `leader` | Transfert du commandement | 5 | `{player}` `{faction}` |
| `title` | Titres des membres | 3 | `{player}` `{title}` `{max}` |
| `relation` | Alliances et ruptures | 20 | `{relation}` `{faction}` `{label}` `{command}` `{other}` `{max}` `{time}` `{count}` `{factions}` |
| `home` | Home | 9 | `{player}` |
| `warp` | Warps | 16 | `{warp}` `{max}` `{label}` `{faction}` `{count}` `{protected}` |
| `teleport` | Attente, annulation et délai des téléportations | 7 | `{seconds}` |
| `chat` | Canaux de discussion | 7 | `{mode}` |
| `economy` | Paiements Vault | 5 | `{cost}` `{amount}` `{player}` |
| `chest` | Coffre de faction | 2 | `{label}` |
| `seechunk` | /f seechunk | 2 |  |
| `top` | /f top et noms des catégories | 9 | `{category}` `{page}` `{pages}` `{rank}` `{relation_color}` `{faction}` `{value}` `{categories}` |
| `power` | Puissance | 8 | `{power}` `{max_power}` `{player}` `{loss}` `{gain}` |
| `quests` | /f quests quand les quêtes ne sont pas disponibles | 1 |  |
| `points` | Points de faction (commandes admin) | 1 | `{faction}` `{points}` |
| `perms` | /f perms | 10 | `{action}` `{subject}` `{state}` `{player}` |
| `flags` | /f flags | 11 | `{flag}` `{state}` `{faction}` `{description}` |
| `access` | /f access | 8 | `{name}` `{x}` `{z}` `{players}` `{factions}` |
| `upgrades` | Améliorations | 6 | `{upgrade}` `{level}` `{player}` `{cost}` `{faction}` |
| `logs` | Journal de faction et texte de chaque type d'entrée | 28 | `{faction}` `{page}` `{pages}` `{date}` `{message}` `{player}` `{actor}` `{role}` `{count}` `{x}` `{z}` `{relation}` `{warp}` `{upgrade}` `{level}` `{flag}` `{state}` `{action}` `{subject}` |
| `announce` | Annonces | 4 | `{player}` `{message}` `{max}` |
| `coords` | Partage de position | 1 | `{player}` `{world}` `{x}` `{y}` `{z}` |
| `toggle` | Préférences /f toggle | 5 |  |
| `territory` | Notifications d'entrée dans un territoire | 6 | `{color}` `{faction}` `{description}` |
| `protection` | Refus de protection, message par action | 7 | `{faction}` `{command}` |
| `pvp` | Refus de PvP | 10 |  |
| `admin` | Commandes d'administration | 7 | `{player}` `{faction}` `{version}` `{storage}` `{factions}` `{players}` `{claims}` `{vault}` `{papi}` |
| `inactivity` | Expulsions et dissolutions pour inactivité | 3 | `{player}` `{faction}` |
| `member-login` | Connexion d'un membre | 1 | `{player}` |
| `member-logout` | Déconnexion d'un membre | 1 | `{player}` |
| `config-editor` | Éditeur /f config | 20 | `{path}` `{empty}` `{cancel}` `{file}` `{value}` `{input}` `{key}` `{error}` |

## 6. `menus.json`

Tout ce qui apparaît dans les menus vient de ce fichier, rien n'est imposé par le plugin.

### Un menu

| Clé | Rôle |
|---|---|
| `titre` | titre de l'inventaire (32 caractères maximum, couleurs comprises) |
| `lignes` | hauteur, de 1 à 6 |
| `fond` | objet qui remplit les cases vides (`"materiau": "AIR"` pour un fond vide) |
| `objets` | objets libres, ajoutables et supprimables |
| `cases`, `modele-...` | menus en liste (membres, warps, flags...) : cases utilisées et apparence de chaque élément |

### Un objet

| Clé | Rôle |
|---|---|
| `case` | emplacement, de 0 (en haut à gauche) à 53 ; ligne 2 = cases 9 à 17, etc. |
| `materiau` | matériau ; `{icone}` reprend l'icône de l'élément affiché |
| `quantite` | nombre affiché (1 à 64) |
| `nom`, `description` | nom et lignes (une variable liste donne une ligne par élément) |
| `brillant` | effet d'enchantement |
| `tete` | pseudo pour une tête de joueur (`SKULL_ITEM:3`) |
| `action` ou `clic` | effet de tous les clics |
| `clic-gauche`, `clic-droit`, `clic-milieu`, `shift-clic`, `shift-clic-gauche`, `shift-clic-droit`, `touche-q`, `ctrl-q` | effet d'un clic précis (prioritaire sur `clic`) |

Un effet est une action ou une liste d'actions exécutées dans l'ordre :

| Action | Effet |
|---|---|
| `COMMANDE:<commande>` | le joueur exécute la commande (`{label}` = `f`) |
| `CONSOLE:<commande>` | la console exécute la commande |
| `MENU:<menu>` | ouvre un menu : `principal`, `membres`, `permissions`, `permissions:<ROLE ou ALLY>`, `permissions-joueurs`, `permissions-joueur:<pseudo>`, `flags`, `ameliorations`, `warps`, `config`, `config:<fichier>` |
| `MESSAGE:<texte>` | envoie un texte au joueur |
| `FERMER`, `ACTUALISER` | ferme / redessine le menu |
| `PAGE-SUIVANTE`, `PAGE-PRECEDENTE` | pages des menus en liste |
| `BASCULER` | flags et permissions : active / désactive |
| `REINITIALISER` | permission d'un joueur : revient à celle de son rôle |
| `ACHETER` | améliorations : achète le niveau suivant |

### Menus par défaut

### Menu `principal`

Menu /f menu d'un joueur en faction. Les objets sont libres : ajoutez-en, supprimez-en.

| Clé | Défaut |
|---|---|
| `titre` | `"&8Faction &7» &6{faction}"` |
| `lignes` | `5` |

**`fond`** : `materiau`, `nom`

**`objets`** (objets libres) :

| Objet | Case | Matériau | Nom | Clic |
|---|---|---|---|---|
| `infos` | `4` | `"BANNER:1"` | `"&6&l{faction}"` | `"COMMANDE:{label} show"` |
| `quetes` | `13` | `"BOOK_AND_QUILL"` | `"&bQuêtes de faction"` | `"COMMANDE:{label} quests"` |
| `membres` | `19` | `"SKULL_ITEM:3"` | `"&eMembres"` | `"MENU:membres"` |
| `permissions` | `20` | `"COMMAND"` | `"&ePermissions"` | `"MENU:permissions"` |
| `flags` | `21` | `"BANNER:10"` | `"&eFlags"` | `"MENU:flags"` |
| `ameliorations` | `22` | `"EXP_BOTTLE"` | `"&eAméliorations"` | `"MENU:ameliorations"` |
| `warps` | `23` | `"EYE_OF_ENDER"` | `"&eWarps"` | `"MENU:warps"` |
| `coffre` | `24` | `"ENDER_CHEST"` | `"&eCoffre de faction"` | `"COMMANDE:{label} chest"` |
| `home` | `25` | `"BED"` | `"&eHome"` | `["FERMER", "COMMANDE:{label} home"]` |
| `carte` | `30` | `"MAP"` | `"&eCarte"` | `["FERMER", "COMMANDE:{label} map"]` |
| `relations` | `31` | `"GOLD_SWORD"` | `"&eRelations"` | `["FERMER", "COMMANDE:{label} relations"]` |
| `classement` | `32` | `"GOLD_INGOT"` | `"&eClassement"` | `["FERMER", "COMMANDE:{label} top"]` |
| `fermer` | `40` | `"BARRIER"` | `"&cFermer"` | `"FERMER"` |

Variables utilisées par défaut : `{faction}` `{description}` `{leader}` `{members_online}` `{members}` `{power}` `{max_power}` `{points}` `{claims}` `{max_claims}` `{player}`

### Menu `sans-faction`

Menu /f menu d'un joueur sans faction. Les objets sont libres.

| Clé | Défaut |
|---|---|
| `titre` | `"&8Factions"` |
| `lignes` | `3` |

**`fond`** : `materiau`, `nom`

**`objets`** (objets libres) :

| Objet | Case | Matériau | Nom | Clic |
|---|---|---|---|---|
| `creer` | `11` | `"BANNER:14"` | `"&aCréer une faction"` | `["FERMER", "MESSAGE:&7Tapez &f/{label} create <nom> &7pour créer votre faction."]` |
| `liste` | `13` | `"BOOK"` | `"&eListe des factions"` | `["FERMER", "COMMANDE:{label} list"]` |
| `classement` | `15` | `"GOLD_INGOT"` | `"&eClassement"` | `["FERMER", "COMMANDE:{label} top"]` |

### Menu `membres`

Liste des membres : cases utilisées, modèle d'un membre et boutons de page.

| Clé | Défaut |
|---|---|
| `titre` | `"&8Membres &7» &6{faction}"` |
| `lignes` | `5` |
| `cases` | `[10,11,12,13,14,15,16,19, …]` (21) |

**`fond`** : `materiau`, `nom`

**`modele-membre`** : `materiau`, `tete`, `nom`, `description`, `clic-gauche`, `clic-droit`, `shift-clic`, `statut-en-ligne`, `statut-hors-ligne`

**`precedent`** : `case`, `materiau`, `nom`, `action`

**`suivant`** : `case`, `materiau`, `nom`, `action`

**`retour`** : `case`, `materiau`, `nom`, `action`

Variables utilisées par défaut : `{faction}` `{member}` `{role_color}` `{role_prefix}` `{role}` `{title}` `{power}` `{max_power}` `{status}` `{last_seen}`

### Menu `permissions`

Menus de /f perms : choix (objets), actions d'un rôle, liste des joueurs, actions d'un joueur.

| Clé | Défaut |
|---|---|
| `titre-choix` | `"&8Permissions &7» Choix"` |
| `lignes-choix` | `3` |
| `titre-actions` | `"&8Permissions &7» {subject}"` |
| `lignes-actions` | `6` |
| `cases-actions` | `[0,1,2,3,4,5,6,7, …]` (45) |
| `titre-joueurs` | `"&8Permissions &7» Joueurs"` |
| `lignes-joueurs` | `5` |
| `cases-joueurs` | `[10,11,12,13,14,15,16,19, …]` (21) |
| `titre-actions-joueur` | `"&8Perms &7» {member}"` |

**`fond`** : `materiau`, `nom`

**`objets`** (objets libres) :

| Objet | Case | Matériau | Nom | Clic |
|---|---|---|---|---|
| `COLEADER` | `10` | `"GOLD_HELMET"` | `"&6Co-Chefs"` | `"MENU:permissions:COLEADER"` |
| `OFFICER` | `11` | `"IRON_HELMET"` | `"&eOfficiers"` | `"MENU:permissions:OFFICER"` |
| `MEMBER` | `12` | `"CHAINMAIL_HELMET"` | `"&aMembres"` | `"MENU:permissions:MEMBER"` |
| `RECRUIT` | `13` | `"LEATHER_HELMET"` | `"&7Recrues"` | `"MENU:permissions:RECRUIT"` |
| `ALLY` | `15` | `"WOOL:2"` | `"&dAlliés"` | `"MENU:permissions:ALLY"` |
| `joueurs` | `16` | `"SKULL_ITEM:3"` | `"&bPar joueur"` | `"MENU:permissions-joueurs"` |
| `retour` | `22` | `"BARRIER"` | `"&cRetour"` | `"MENU:principal"` |

**`modele-action`** : `materiau-autorise`, `materiau-refuse`, `brillant-si-autorise`, `nom`, `description`, `etat-oui`, `etat-non`, `texte-verrouille`, `clic`

**`retour-actions`** : `case`, `materiau`, `nom`, `action`

**`modele-joueur`** : `materiau`, `tete`, `nom`, `description`, `clic`

**`joueurs-precedent`** : `case`, `materiau`, `nom`, `action`

**`joueurs-suivant`** : `case`, `materiau`, `nom`, `action`

**`joueurs-retour`** : `case`, `materiau`, `nom`, `action`

**`modele-action-joueur`** : `materiau-autorise`, `materiau-refuse`, `brillant-si-personnalise`, `nom`, `description`, `etat-oui`, `etat-non`, `etat-herite`, `texte-verrouille`, `clic-gauche`, `clic-droit`

**`retour-actions-joueur`** : `case`, `materiau`, `nom`, `action`

Variables utilisées par défaut : `{player}` `{subject}` `{icone}` `{state_color}` `{action}` `{description}` `{state}` `{locked}` `{member}` `{role_color}` `{role}` `{custom}` `{role_state}`

### Menu `flags`

Menu /f flags. Modèle d'un flag : materiau ({icone} = icône du flag), brillant-si-actif.

| Clé | Défaut |
|---|---|
| `titre` | `"&8Flags &7» &6{faction}"` |
| `lignes` | `4` |
| `cases` | `[10,11,12,13,14,15,16,19, …]` (14) |

**`fond`** : `materiau`, `nom`

**`modele-flag`** : `materiau`, `brillant-si-actif`, `nom`, `description`, `etat-oui`, `etat-non`, `texte-modifiable`, `texte-verrouille`, `clic`

**`retour`** : `case`, `materiau`, `nom`, `action`

Variables utilisées par défaut : `{faction}` `{icone}` `{state_color}` `{flag}` `{description}` `{state}` `{editable}`

### Menu `ameliorations`

Menu /f upgrades. Chaque amélioration est placée à sa case (factions.json) avec ce modèle ; materiau {icone} = icône de l'amélioration.

| Clé | Défaut |
|---|---|
| `titre` | `"&8Améliorations &7» &6{faction}"` |
| `lignes` | `4` |

**`fond`** : `materiau`, `nom`

**`modele-amelioration`** : `materiau`, `brillant-si-max`, `nom`, `description`, `statut-disponible`, `statut-max`, `statut-sans-permission`, `aucun`, `clic`

**`info-argent`** : `case`, `materiau`, `nom`

**`retour`** : `case`, `materiau`, `nom`, `action`

Variables utilisées par défaut : `{faction}` `{icone}` `{name}` `{level}` `{max_level}` `{description}` `{current}` `{next}` `{cost}` `{status}` `{balance}`

### Menu `warps`

Menu des warps : cases utilisées et modèle d'un warp.

| Clé | Défaut |
|---|---|
| `titre` | `"&8Warps &7» &6{faction}"` |
| `lignes` | `3` |
| `cases` | `[10,11,12,13,14,15,16]` |

**`fond`** : `materiau`, `nom`

**`modele-warp`** : `materiau`, `nom`, `description`, `mot-de-passe-oui`, `mot-de-passe-non`, `clic-gauche`, `shift-clic-droit`

**`vide`** : `case`, `materiau`, `nom`, `description`

**`retour`** : `case`, `materiau`, `nom`, `action`

Variables utilisées par défaut : `{faction}` `{warp}` `{world}` `{x}` `{y}` `{z}` `{password}`

### Menu `editeur`

Éditeur /f config : fichiers proposés, apparence de chaque type d'option (types) et boutons.

| Clé | Défaut |
|---|---|
| `titre-fichiers` | `"&8Configuration"` |
| `lignes-fichiers` | `3` |
| `titre-section` | `"&8{file} &7» &f{section}"` |
| `lignes-section` | `6` |
| `cases-options` | `[0,1,2,3,4,5,6,7, …]` (45) |
| `texte-defaut` | `"&8(valeur par défaut, absente du fichier)"` |
| `texte-reinitialiser` | `"&8Ctrl + Q : revenir à la valeur par défaut"` |
| `texte-supprimer` | `"&cCtrl + Q : supprimer cette entrée"` |
| `mot-de-passe-masque` | `"******"` |
| `liste-lignes-max` | `8` |
| `liste-suite` | `"&8..."` |

**`fond`** : `materiau`, `nom`

**`fichiers`** (objets libres) :

| Objet | Case | Matériau | Nom | Clic |
|---|---|---|---|---|
| `config` | `11` | `"COMMAND"` | `"&6config.json"` | `"MENU:config:config"` |
| `factions` | `12` | `"BANNER:1"` | `"&6factions.json"` | `"MENU:config:factions"` |
| `territoire` | `13` | `"GRASS"` | `"&6territoire.json"` | `"MENU:config:territoire"` |
| `messages` | `14` | `"BOOK_AND_QUILL"` | `"&6messages.json"` | `"MENU:config:messages"` |
| `menus` | `15` | `"CHEST"` | `"&6menus.json"` | `"MENU:config:menus"` |
| `fermer` | `22` | `"BARRIER"` | `"&cFermer"` | `"FERMER"` |

**`types`** : `section`, `section-libre`, `oui`, `non`, `nombre`, `texte`, `choix`, `liste`, `complexe`

**`info`** : `case`, `materiau`, `nom`, `description`

**`retour`** : `case`, `materiau`, `nom`, `action`

**`precedent`** : `case`, `materiau`, `nom`, `action`

**`suivant`** : `case`, `materiau`, `nom`, `action`

**`ajouter`** : `case`, `materiau`, `nom`, `description`, `action`

Variables utilisées par défaut : `{file}` `{section}` `{key}` `{doc}` `{count}` `{effacer}` `{defaut}` `{value}` `{step}` `{big}` `{choices}` `{list}` `{path}`


## 7. Tâches courantes

| Besoin | Réglage |
|---|---|
| Changer `/f` en `/fac` | `config.json` → `commande.nom`, puis redémarrer |
| Changer ce qu'ouvre `/f quests` | `commandes.quests.commande` (par défaut `quetes faction`, les quêtes de faction de KoriaQuest) |
| Désactiver une commande | `commandes.<commande>.actif` → `false` |
| Réserver une commande à un grade | `commandes.<commande>.permission` → une permission donnée à ce grade |
| Ajouter un délai à `/f home` | `commandes.home.delai` (entre deux commandes) ou `teleportation.delai-secondes.home` |
| Passer en MySQL / MariaDB | `stockage.type` → `MYSQL`, remplir `stockage.mysql`, redémarrer (les données JSON ne sont pas copiées automatiquement) |
| Interdire les claims dans un monde | `territoire.json` → `claims.mondes` (mode `BLACKLIST`) |
| Rendre une faction permanente | `/f setflag <faction> PERMANENT on` |
| Ajouter un grade de chat | copier une entrée de `chat.grades` avec sa permission, la placer selon son rang |
| Bloquer une commande en territoire ennemi | `protection.commandes-bloquees.ENEMY` (sans le `/`) |
| Changer un texte | `messages.json`, ou `/f config` → messages |
| Déplacer un bouton du menu | `menus.json` → `principal.objets.<objet>.case` |
| Revenir aux valeurs d'origine | supprimer l'option (ou le fichier) puis `/f reload` |
