# Arcane Horizon — 0.4.0-alpha

Minecraft **26.3**, NeoForge **26.3.0.102**, JDK **25 x64**.
Projet indépendant SworldCraft. Aucun code de Distant Horizons n'est inclus.

## Feuillage simplifié et cellules manquantes

Cette version sépare le sol et la cime pour les colonnes où des feuilles ont
réellement été observées, y compris une couche de neige posée sur des feuilles.
Le sol est recherché sous le feuillage et les troncs, avec une recherche bornée.
La hauteur, la couleur et la lumière de la cime sont conservées séparément.

Les couronnes sont regroupées par cellules stables de 4 blocs, puis 8 au loin.
Elles ont un petit volume fermé : forme plus pointue pour les sapins, plus arrondie
pour les autres feuillages. Une couche de neige simplifiée est ajoutée quand la
majorité des échantillons du groupe en portaient. Une tige de support est approximée
au centre du groupe pour éviter une cime détachée du sol.

Ce n'est pas une copie exacte des arbres : plusieurs branches ou arbres peuvent
être regroupés. Les supports sont approximatifs, pas des troncs précisément localisés.
Aucun feuillage n'est inventé sur une tuile qui n'en contient pas dans les relevés.
La géométrie est opaque, sans texture ni transparence des feuilles. Le nouvel aspect
et son coût graphique doivent être mesurés dans le test en jeu.

Le maillage du sol corrige aussi un cas précis : une cellule contenant quelques
colonnes connues pouvait être entièrement ignorée si la première fenêtre de calcul
d'un coin était vide. Le calcul s'étend maintenant de façon symétrique, en utilisant
uniquement les colonnes connues. Les coins communs restent identiques.
Cela ne remplit pas les chunks jamais mémorisés et ne garantit pas la disparition
de tous les trous, en particulier aux limites de capture ou sous les surplombs.

### Test ciblé de cette version

- Vérifier `/arcane status` : version **0.4.0-alpha**.
- Explorer une forêt à 12 chunks, attendre la capture, puis passer à 4 sans quitter.
- Comparer le feuillage depuis une position fixe avec `/arcane foliage`.
  Refaire la commande pour le réactiver. Le réglage est activé par défaut.
- Vérifier en particulier les sapins, les cimes enneigées et les raccords du sol.
- Le cache repart à zéro après le redémarrage, il faut donc explorer à nouveau.

La compilation Java et les tests CPU ont été exécutés, notamment les cellules
peu remplies, l'absence d'arbres inventés sur une tuile vide, la stabilité du feuillage,
les limites de hauteur, l'éclairage et la simplification au loin. Le rendu GPU n'a
pas été lancé dans cet environnement. La capture Minecraft et la séparation du sol
sont à confirmer sur votre monde.

## Correctif lumière jour/nuit

Le rendu précédent utilisait un shader de couleur sans lightmap : la neige pouvait
rester blanche la nuit alors que les chunks normaux étaient sombres.

Cette version ajoute :
- des relevés séparés de lumière du ciel et de lumière des blocs au-dessus de la surface ;
- leur transmission dans les attributs lumineux du maillage ;
- un vertex shader propre au projet, qui multiplie la couleur par la lightmap du jeu ;
- la liaison explicite de cette lightmap dans notre pipeline.

La lightmap du jeu est actualisée sans devoir reconstruire le relief à chaque
changement d'heure. Le shader réutilise les interfaces GLSL publiques du jeu par
inclusion ; leurs sources ne sont pas copiées dans le projet.
Ce correctif ne traite pas les éventuels trous, l'éclairage exact des faces,
les textures, la transparence de l'eau ou le brouillard.

### Test ciblé

Dans un monde de test avec les commandes disponibles, conserver la même position :

```text
/time set day
/time set night
```

Le relief simplifié doit s'assombrir avec le terrain normal. Vérifier d'abord
`/arcane status` : la version doit être **0.4.0-alpha**.
La compilation GPU du nouveau shader et son résultat visuel restent à confirmer en jeu.
Les tests CPU ne valident pas un shader sur la carte graphique.

## Changement ciblé après les plaques suspendues

Cette version remplace le maillage en colonnes et ses parois de bord par une
**surface continue**. Les coins sont calculés dans les coordonnées du monde,
en moyennant les colonnes connues autour du même point. Deux chunks voisins
utilisent donc exactement la même hauteur sur leur bord commun.

- Aucun mur vertical artificiel, aucune jupe de bord ajoutée au maillage.
- Grille uniforme de 4 blocs ; passage uniforme à 8 si le cache dépasse le budget
  de départ. Ne pas mélanger les pas de grille évite les fissures entre niveaux de détail.
- Les maillages sont réutilisés, puis reconstruits si une tuile voisine change.
- Les bornes utilisées pour l'élimination hors champ viennent du maillage final.
- Les couleurs et commandes de réglage de 0.3.1 sont conservées.

Il s'agit d'une surface simplifiée et plus lissée, pas d'une reconstruction 3D
fidèle des arbres ou constructions. Ces modifications retirent deux mécanismes
pouvant créer des raccords incorrects. Elles ne prouvent pas, à elles seules,
que tous les artefacts observés en jeu ont disparu.

## Installation et test

1. Fermer le Minecraft de test.
2. Copier **le contenu du dossier ArcaneHorizon du ZIP** dans le dossier de votre
   projet existant contenant `build.gradle`. Accepter le remplacement des fichiers.
3. Dans PowerShell, depuis ce dossier :

```powershell
.\gradlew.bat build
.\gradlew.bat runClient
```

4. En jeu, `/arcane status` doit indiquer **0.4.0-alpha**.
5. Sans shader, explorer une zone à 12 chunks puis, dans la même session,
   réduire à 4 chunks. Vérifier particulièrement les raccords en tournant la caméra.
6. `/arcane render` permet de comparer activé/désactivé depuis une position fixe.

Le JAR après un build réussi : `build/libs/arcanehorizon-0.4.0-alpha.jar`.

## Commandes

| Commande | Effet |
| --- | --- |
| `/arcane status` | Version, cache, capture, tuiles et sommets sélectionnés |
| `/arcane sample` | Altitudes du chunk sous le joueur |
| `/arcane render` | Activer/désactiver le rendu |
| `/arcane toggle` | Suspendre/reprendre la capture |
| `/arcane range 32` | Portée demandée de 8 à 48 chunks |
| `/arcane preview` | Diagnostic de recouvrement des chunks proches |
| `/arcane brightness 0.95` | Luminosité, de 0,2 à 1,5 |
| `/arcane saturation 0.85` | Saturation, de 0 à 1,5 |
| `/arcane overlap 3` | Bande de recouvrement proche, de 0 à 8 chunks |
| `/arcane culling` | Basculer l'élimination hors champ pour diagnostic |
| `/arcane foliage` | Activer/désactiver les couronnes simplifiées |
| `/arcane clear` | Vider le cache et la file de capture |

Réglages conservés seulement pendant la session. Un nouveau lancement repart
avec un cache vide : explorer à nouveau avant de réduire la distance.

## Limites

Cache en mémoire, limité à 8192 tuiles. Pas de génération distante ni sauvegarde
entre sessions ou dimensions. Les zones inconnues restent absentes.
La portée reste limitée par la projection du jeu. Le choix de grille est global
et peut changer lorsque le cache grandit. Le budget est souple, sans suppression
arbitraire des tuiles candidates.

Le sol reste un relief de surface. Les troncs de support du feuillage sont approximatifs ;
les grottes, passages sous les constructions et surplombs ne sont pas reconstitués.
Les falaises peuvent être adoucies. Pas de textures, transparence de l'eau ou brouillard adapté. Les couleurs restent approximatives.
Compatibilité shaders/autres moteurs et performances non validées.

## Vérifications effectuées

Les tests suivants ont été exécutés avec Java 25 et ont réussi :
- raccords est/ouest et nord/sud entre chunks de relief non uniforme ;
- raccords aux coordonnées négatives ;
- couverture complète d'un chunk non vide ;
- absence de faces verticales artificielles ;
- reconstruction lorsqu'un voisin apparaît et réutilisation sinon ;
- absence de géométrie pour l'air, conservation du blanc et immutabilité.

Toutes les classes Java ont été compilées directement contre Minecraft 26.3
et les dépendances NeoForge 26.3.0.102, avec un JAR temporaire reproduisant deux
adaptations de NeoForge : héritage de BaseRenderState et visibilité publique de
RenderType.create. Ce JAR temporaire n'est pas livré.

Le build Gradle complet demeure non validé dans cet environnement à cause du
téléchargement de préparation Minecraft. Aucun test graphique ni compilation GPU du shader n'a été effectué ici.
Les tests ne remplacent pas la vérification visuelle sur votre PC.

Le correctif `failOnNoDiscoveredTests = false` est inclus ; les vérifications
propres au projet restent lancées par `meshCheck` pendant `build`.

## Provenance

Voir `NOTICE.md` et `TEMPLATE_LICENSE.txt`. Aucun envoi GitHub n'a été effectué.
Le fichier RenderPlanner.java conservé pour faciliter la mise à jour des dépôts
existants n'est plus utilisé par le chemin de rendu principal.
