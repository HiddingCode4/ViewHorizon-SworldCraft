# Arcane Horizon — 0.3.3-alpha

Minecraft **26.3**, NeoForge **26.3.0.102**, JDK **25 x64**.
Projet indépendant SworldCraft. Aucun code de Distant Horizons n'est inclus.

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
`/arcane status` : la version doit être **0.3.3-alpha**.
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

4. En jeu, `/arcane status` doit indiquer **0.3.3-alpha**.
5. Sans shader, explorer une zone à 12 chunks puis, dans la même session,
   réduire à 4 chunks. Vérifier particulièrement les raccords en tournant la caméra.
6. `/arcane render` permet de comparer activé/désactivé depuis une position fixe.

Le JAR après un build réussi : `build/libs/arcanehorizon-0.3.3-alpha.jar`.

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
| `/arcane clear` | Vider le cache et la file de capture |

Réglages conservés seulement pendant la session. Un nouveau lancement repart
avec un cache vide : explorer à nouveau avant de réduire la distance.

## Limites

Cache en mémoire, limité à 8192 tuiles. Pas de génération distante ni sauvegarde
entre sessions ou dimensions. Les zones inconnues restent absentes.
La portée reste limitée par la projection du jeu. Le choix de grille est global
et peut changer lorsque le cache grandit. Le budget est souple, sans suppression
arbitraire des tuiles candidates.

Le relief de surface représente les sommets d'arbres comme des hauteurs moyennées.
Il ne reconstitue ni troncs, ni passages dessous, ni grottes, ni surplombs.
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
