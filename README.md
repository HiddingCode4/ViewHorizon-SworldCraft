# Arcane Horizon — 0.3.1-alpha

Prototype indépendant SworldCraft — Minecraft **26.3**, NeoForge **26.3.0.102**, **JDK 25 x64**.

## Correctif de continuité et de couleurs

- La limite qui ne gardait que les 512 tuiles les plus proches est supprimée.
  Toutes les tuiles candidates sont conservées. Au-delà d'un budget souple de
  240 000 sommets, les tuiles les plus éloignées passent à un niveau moins détaillé.
  Si même le niveau le plus grossier dépasse ce budget, la couverture reste prioritaire.
  Cela corrige une cause de terrain absent dans notre code, sans garantir tous les raccords.
- Feuillage : la couleur de biome brute est atténuée pour approximer la multiplication
  par la texture des feuilles qui manquait. Facteur de départ : 0,55 ; herbe : 0,78.
  Ce sont nos approximations, pas des valeurs copiées de Distant Horizons.
- Réglages de rendu en session : luminosité **0,95**, saturation **0,85**,
  recouvrement du bord proche **3 chunks** par défaut.
- Les bornes verticales des tuiles sont mises en cache pour éviter leur recalcul par image.

La documentation de l'API Distant Horizons décrit des réglages de luminosité,
saturation, rayon de prévention du recouvrement et élimination hors champ :
https://distant-horizons-team.gitlab.io/distant-horizons/com/seibel/distanthorizons/api/interfaces/config/client/IDhApiGraphicsConfig.html
Notre implémentation est indépendante : son fichier de configuration ne peut pas
être chargé directement ici et les valeurs ne sont pas interchangeables.

### Réglages facultatifs en jeu

| Commande | Effet |
| --- | --- |
| `/arcane brightness 0.95` | Multiplier la luminosité (0,2 à 1,5) |
| `/arcane saturation 0.85` | Régler la saturation ; 0 produit du gris (0 à 1,5) |
| `/arcane overlap 3` | Régler la bande de raccord proche (0 à 8 chunks) |
| `/arcane culling` | Activer/désactiver l'élimination hors champ, pour diagnostic |

Ces valeurs sont appliquées immédiatement et sont réinitialisées au redémarrage.
Une bande de raccord plus large peut cacher des trous de transition, mais augmente
les recouvrements avec les blocs proches. Le budget souple peut aussi coûter plus
que l'ancienne limite stricte ; les FPS restent à mesurer en jeu.

**Les zones inconnues ou absentes du cache ne sont toujours pas inventées.**
Si un trou correspond à un chunk jamais mémorisé, ces réglages ne pourront pas le remplir.
La sauvegarde et la génération à distance ne sont pas incluses dans ce correctif.
Les changements de niveau de détail, la transparence de l'eau et le relief sous les
surplombs restent des limites du prototype.

## Améliorations après le premier test visuel

- Capture des **256 colonnes** de chaque chunk, au lieu de seulement 16 cellules.
- Trois niveaux de détail : cellules de **2 × 2 blocs** à proximité (64 cellules
  par chunk), 4 × 4 à distance intermédiaire et 8 × 8 plus loin.
- Couleurs de carte des blocs, complétées par les teintes de biome pour l'herbe,
  plusieurs feuillages et l'eau. C'est une meilleure approximation, pas les textures des blocs.
- Capture prioritaire des chunks reçus et déchargés. Rafraîchissement périodique
  dans un rayon adapté à la distance d'affichage, jusqu'à 32 chunks.
- Budget de capture : au plus 4 chunks par tick ; une limite souple de 3 ms
  interrompt la boucle après le chunk en cours. Un chunk ne peut pas être interrompu.
- Bande de transition : le relief simplifié couvre aussi le bord de l'affichage normal,
  au lieu d'attendre que chaque chunk disparaisse du cache client. Cette bande peut encore
  provoquer des recouvrements, mais réduit une cause possible de trous.
- Élimination des tuiles hors du champ de vision. Le budget graphique réduit les
  niveaux de détail au lieu de supprimer des tuiles déjà sélectionnées.
- Correctif Gradle `failOnNoDiscoveredTests = false` inclus. Les contrôles de géométrie
  restent exécutés par la tâche `meshCheck` pendant `build`.

Aucun code de Distant Horizons n'est inclus. Le nom est encore provisoire.

## Installer la mise à jour

Fermer le Minecraft de test. Copier **le contenu du dossier ArcaneHorizon du ZIP**
dans votre dossier de projet existant contenant `build.gradle`, en acceptant le
remplacement des fichiers. Votre dossier `.git` n'est pas inclus dans l'archive.

Dans PowerShell, depuis la racine du projet :

```powershell
.\gradlew.bat build
.\gradlew.bat runClient
```

Le JAR produit par un build réussi est `build/libs/arcanehorizon-0.3.1-alpha.jar`.

## Comparaison visuelle dans une même session

1. Démarrer sans shader ni autre mod de rendu, dans un monde de test.
2. Régler la distance normale à **12 chunks**, puis attendre environ 30 secondes.
3. Survoler lentement une zone en faisant des pauses pour mémoriser le relief.
4. Vérifier `/arcane status` : version 0.3.1-alpha, cache non vide, capture active.
5. Sans quitter le monde, passer à **4 chunks** puis attendre quelques secondes.
6. Depuis une position fixe, utiliser `/arcane render` pour comparer le paysage
   avec/sans le relief mémorisé. Refaire la commande pour le réactiver.
7. Comparer surtout les raccords, la couverture mémorisée et les couleurs.
   Les grandes zones jamais parcourues restent nécessairement absentes.

## Commandes

| Commande | Effet |
| --- | --- |
| `/arcane status` | Cache, colonnes, relevés, capture en attente et tuiles sélectionnées |
| `/arcane sample` | Altitudes et premier matériau du chunk sous le joueur |
| `/arcane render` | Activer/désactiver le rendu |
| `/arcane toggle` | Suspendre/reprendre la capture |
| `/arcane range 32` | Portée demandée entre 8 et 48 chunks, limitée par la projection du jeu |
| `/arcane preview` | Diagnostic : afficher aussi les reliefs sur les chunks proches chargés |
| `/arcane clear` | Vider cache et file de capture ; le balayage périodique peut les remplir à nouveau |

`preview` est désactivé par défaut. Il produit volontairement des recouvrements
pour vérifier la géométrie ; le désactiver après diagnostic.

## Limites restantes

Le cache de 8192 tuiles est uniquement en mémoire et repart à zéro après une
nouvelle session ou un changement de monde/dimension. Pas de génération distante.
La file de capture est limitée à 2048 références de chunks ; en cas de déplacement
très rapide ou de surcharge, des zones peuvent encore manquer.

Ce moteur représente uniquement la surface supérieure : les grottes, passages
sous les constructions, façades et dessous des arbres ne sont pas fidèles.
Les cellules plus grossières regroupent les hauteurs par maximum et moyennent les
couleurs ; des paliers et changements de détail peuvent rester visibles.
Les parois externes descendent sous le minimum du chunk pour masquer les bords,
sans garantir tous les raccords entre chunks de hauteurs différentes.

La transition n'est pas encore fondue, l'eau est opaque, les couleurs sont
approximatives et l'éclairage est une ombre directionnelle simple. Pas de textures
ni de brouillard lointain adaptatif. Ne pas attendre la qualité d'un moteur abouti.
Compatibilité shaders et autres moteurs non validée ; performances à mesurer en jeu.

## Vérifications avant livraison

- Tests de géométrie exécutés avec Java 25 : surfaces, parois, coordonnées,
  opacité, cellules d'air et immutabilité des tuiles — réussis.
- Toutes les classes Java compilées directement avec Java 25, les classes
  Minecraft 26.3 et les dépendances NeoForge 26.3.0.102. Pour ce contrôle,
  deux adaptations d'API documentées de NeoForge ont été appliquées dans un
  JAR temporaire : héritage de `BaseRenderState` et accès public à
  `RenderType.create`. Ce JAR de vérification n'est pas livré.
- Le `build` Gradle complet reste non validé ici : le téléchargement de
  préparation Minecraft par NeoForm échoue dans cet environnement réseau.
- Aucun test graphique en jeu n'a été exécuté ici. Le protocole ci-dessus
  reste nécessaire pour confirmer le rendu réel.

## GitHub et provenance

Copier les fichiers dans le dépôt existant puis enregistrer les modifications depuis VS Code.
Aucun envoi GitHub n'a été effectué. Voir `NOTICE.md` et `TEMPLATE_LICENSE.txt`.
