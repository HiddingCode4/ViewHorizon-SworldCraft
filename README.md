# Arcane Horizon — prototype 0.1.0-alpha

Projet independant SworldCraft. Nom provisoire.
Minecraft **26.3**, NeoForge **26.3.0.102**, JDK **25 64 bits**.

## Ce que teste cette version

- Chargement de notre mod et commandes clientes `/arcane`.
- Capture progressive des chunks deja charges, dans un rayon de 8 chunks autour du joueur.
- Reduction de chaque chunk en 16 cellules de surface de 4 x 4 blocs : altitude maximale et identifiant du bloc de surface.
- Un chunk traite par tick ; aucune demande de chargement ou generation de chunks.
- Cache de 8192 tuiles maximum, conserve lors du deplacement, vide au changement de monde/dimension ou deconnexion.

**Cette version ne dessine pas encore de paysages lointains.** Pas de sauvegarde disque,
pas de generation distante, pas de compatibilite shaders validee. Le relief ne represente
pas les grottes ni les surplombs. Le cache est rafraichi progressivement, pas instantanement.

## Installation Windows / VS Code

1. Installer Microsoft OpenJDK **25 x64** (JDK complet), puis redemarrer VS Code.
2. Installer les extensions **Extension Pack for Java** et **Gradle for Java**.
3. Extraire ce dossier dans un chemin simple, par exemple `D:\ArcaneHorizon`.
4. VS Code > Fichier > Ouvrir le dossier > choisir le dossier contenant `build.gradle`.
5. Ouvrir Terminal > Nouveau terminal (PowerShell), puis :

```powershell
java -version
.\gradlew.bat --version
.\gradlew.bat build
.\gradlew.bat runClient
```

Java doit afficher 25. Gradle peut aussi provisionner automatiquement sa toolchain Java 25
via Foojay ; installer le JDK reste recommande pour VS Code. La premiere preparation telecharge
Minecraft et les dependances et peut prendre longtemps. Ne pas interrompre tant que le terminal progresse.
Aucune installation separee de Gradle n'est necessaire : le wrapper est fourni.

## Premier test en jeu

1. Lancer sans shader et sans Distant Horizons dans l'environnement de developpement.
2. Verifier **Arcane Horizon** dans la liste Mods.
3. Creer un monde de test en Creatif. Attendre environ 15 secondes.
4. Entrer `/arcane status` : le nombre de tuiles doit augmenter.
5. Entrer `/arcane sample` : verifier les altitudes et le materiau du chunk sous le joueur.
6. Se deplacer ; les tuiles doivent rester en memoire et de nouvelles doivent s'ajouter.
7. `/arcane toggle` suspend/reprend la capture. `/arcane clear` vide le cache
   (il se remplit de nouveau si la capture reste active).
8. Quitter puis changer de monde : les anciennes tuiles ne doivent pas etre reutilisees.

Le JAR produit se trouve dans `build/libs/arcanehorizon-0.1.0-alpha.jar`.
Pour un test dans ton profil existant, utiliser une copie du profil et ce JAR,
pas le fichier `-sources.jar`. Aucune compatibilite avec les autres mods n'est encore garantie.

## GitHub

Creer un depot vide, sans README ni licence generee automatiquement. Puis dans ce dossier :

```powershell
git init
git add .
git commit -m "Initial independent terrain sampling prototype"
git branch -M main
git remote add origin https://github.com/TON_COMPTE/TON_DEPOT.git
git push -u origin main
```

Remplacer l'URL par celle de ton depot. Aucun envoi GitHub n'a ete fait par l'assistant.
Le code propre du projet est reserve par defaut ; voir NOTICE.md pour les outils tiers.

## Suite

1. Valider compilation et releves sur ton PC.
2. Construire les maillages de terrain et leur raccord avec les chunks normaux.
3. Ajouter le rendu lointain sans shader et mesurer ses performances.
4. Sauvegarde par monde/dimension, puis integration shader ciblee et testee.

## Verification avant livraison

Les evenements utilises ont ete controles dans les sources NeoForge 26.3.0.102.
La structure des metadonnees et les fichiers VS Code ont ete verifies.
La compilation complete n'a pas pu etre executee ici : le telechargement Gradle par Java
a echoue avec `Network is unreachable`. Aucun lancement Minecraft n'a ete effectue.
Le premier `build` sur ton PC doit donc encore confirmer les appels aux API Minecraft.
