# Arcane Horizon — 0.5.0-alpha, intégration Distant Horizons

Minecraft **26.3**, NeoForge **26.3.0.102**, JDK **25 x64**.
Moteur externe : **Distant Horizons 3.3.4-26.3**, version Modrinth `TGgEbP9A`.

## Ce que livre cette version

Arcane Horizon devient ici un **module d'intégration de Distant Horizons**.
La génération distante, les données LOD, leur stockage et leur rendu sont ceux de
Distant Horizons. Les réglages `/arcane` pilotent son API publique.

Ce n'est pas une extraction du générateur dans un moteur autonome. Copier seulement
quelques classes de génération ne remplacerait ni le stockage ni le rendu qui posaient
problème dans notre prototype. Le moteur DH reste donc un mod distinct, identifié avec
ses auteurs et sa licence. Il n'a pas été renommé, fusionné ou présenté comme notre code.

Notre capture, notre maillage et notre shader expérimentaux sont désactivés lorsque
DH est présent, y compris leurs événements et anciennes commandes. Le chargement
client exige DH pour éviter un retour silencieux à l'ancien moteur.
Les sources du prototype sont conservées, mais ne sont pas utilisées pour ce test.

## Installation dans VS Code

1. Fermer Minecraft.
2. Copier **le contenu du dossier ArcaneHorizon du ZIP** dans le dossier existant
   contenant `build.gradle`, en acceptant le remplacement des fichiers.
3. Redémarrer/recharger le projet Gradle dans VS Code si nécessaire.
4. Depuis ce dossier dans PowerShell :

```powershell
.\gradlew.bat build
.\gradlew.bat runClient
```

Gradle télécharge **le vrai moteur Distant Horizons** via le dépôt Maven de Modrinth.
Il est ajouté automatiquement à la session de développement par `localRuntime`.
Ne pas ajouter une deuxième copie de DH dans le dossier de mods de ce test.
Le répertoire de jeu de développement est explicitement `runs/client`.
Si vous aviez des mondes de test dans un autre répertoire, vous pouvez soit en créer
un nouveau, soit copier votre monde de test dans `runs/client/saves` après fermeture.

Aucun binaire DH n'est embarqué dans ce ZIP ni fusionné au JAR Arcane Horizon.
Le build produit `build/libs/arcanehorizon-0.5.0-alpha.jar`.
Dans un profil Minecraft normal, installer séparément **ce JAR et DH 3.3.4 pour
Minecraft 26.3 / NeoForge**. Le JAR Arcane Horizon seul ne suffit plus.

## Premier test

1. Dans Mods, vérifier la présence de **Distant Horizons 3.3.4** et **Arcane Horizon**.
2. Pour ce premier contrôle, rester sans shader ni autre moteur de rendu ajouté.
3. Créer/ouvrir un monde **solo** et attendre que DH ait fini son initialisation.
4. Entrer `/arcane status`. Il doit indiquer :
   - Arcane Horizon **0.5.0-alpha** ;
   - moteur **Distant Horizons 3.3.4** ;
   - ancien moteur **désactivé**.
5. Entrer `/arcane preset` : rayon 64 chunks, rendu et génération activés,
   luminosité et saturation par défaut du vrai moteur.
6. Garder la distance normale de Minecraft à 8–12 chunks et laisser le moteur
   générer ses données. Il n'est plus nécessaire de faire le test 12 → 4 chunks
   pour déclencher notre ancien cache : il n'est plus utilisé.

La génération prend du temps et consomme des ressources ; sa vitesse dépend du
monde et du PC. En multijoueur, une installation client seule ne peut pas générer
fidèlement le terrain inconnu d'un serveur arbitraire. Il faut des données déjà
reçues ou une infrastructure serveur compatible DH.

## Commandes

| Commande | Effet sur le moteur Distant Horizons |
| --- | --- |
| `/arcane status` | Version du moteur, portée, rendu et génération |
| `/arcane preset` | Demander 64 chunks, activer rendu/génération et restaurer ses couleurs par défaut |
| `/arcane range 96` | Demander une portée entre 8 et 512 chunks |
| `/arcane render` | Activer/désactiver son rendu |
| `/arcane generation` | Activer/désactiver sa génération distante |
| `/arcane brightness 1` | Luminosité via son API |
| `/arcane saturation 1` | Saturation via son API |

Les paramètres sont des réglages API de session. Le menu DH reste disponible pour
ses réglages complets et persistants. Le module ne réécrit pas sa configuration au
lancement ; le profil est appliqué seulement quand `/arcane preset` est demandé.
Si DH refuse un réglage, la commande le signale.
Les anciennes commandes liées au cache maison ne sont plus actives dans ce mode.

## Vérifications et limites

- Release officielle 3.3.4-26.3 obtenue par l'API Modrinth ; empreinte SHA-512
  vérifiée contre les métadonnées officielles de cette release.
- Coordonnée Maven de la dépendance et POM contrôlés.
- Toutes les classes Java compilées directement avec Java 25 contre le **JAR
  officiel DH 3.3.4**, Minecraft 26.3 et les dépendances NeoForge 26.3.0.102.
  Deux adaptations d'API NeoForge ont été reproduites dans un JAR temporaire
  de contrôle : BaseRenderState et accès public à RenderType.create.
  Ce JAR temporaire n'est pas livré.
- L'API historique d'activation de génération est dépréciée mais présente dans
  cette release ; cet avertissement n'est pas une erreur de compilation.
- Les tests CPU du prototype conservé continuent à passer, mais ils ne valident
  pas l'exécution de DH ou la résolution Gradle de la nouvelle dépendance.
- Aucun lancement Minecraft ni test GPU de cette intégration n'a été exécuté ici.
  Le build Gradle complet reste à vérifier sur le PC de test.
- Le support shader est celui du moteur officiel et du shader choisi ; aucune
  nouvelle garantie de compatibilité n'est ajoutée par ce module.

## Attribution et portée

Distant Horizons : James Seibel et ses contributeurs.
Source : https://gitlab.com/distant-horizons-team/distant-horizons
Moteur distribué sous GNU LGPL v3, selon ses fichiers de licence.
Page officielle : https://modrinth.com/mod/distanthorizons
Version choisie : https://modrinth.com/mod/distanthorizons/version/TGgEbP9A

Les apports Arcane Horizon restent distincts du moteur externe. Cette intégration
n'accorde pas la propriété exclusive du code de Distant Horizons.
Voir `NOTICE.md` pour les autres outils. Aucun envoi GitHub n'a été effectué.
