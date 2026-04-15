# Red Light Alert ⚠️ (Java)

Prototype jouable en Java/Swing inspiré de **Red Alert**, avec une approche "assets emoji + géométrie".

## Contenu inclus

- Deux factions asymétriques:
  - **Alliés** `🛡️`
  - **Soviétiques** `🔨`
- Carte tactique en grille (16x10).
- Unités emoji avec statistiques détaillées (HP, attaque, portée, vitesse, coût).
- Bâtiments emoji avec économie par tour.
- Rotation de tour automatique + bouton manuel.
- Escarmouche IA simple (déplacement vers cible, attaque si en portée).
- DLC intégrés:
  - **Base Game**
  - **Aftermath**
  - **Counterstrike**
  - **Retaliation**

## Design visuel

- **Emoji** utilisés comme assets principaux:
  - Infanterie `🪖`, roquette `🚀`, char lourd `🦣`, aérien `✈️`, naval `🛥️`, techno Tesla `⚡`, etc.
- **Formes géométriques** pour le reste:
  - Cases de terrain rectangulaires + grille hexagonale suggérée.
  - Unités cerclées (ellipse), bâtiments en rectangles arrondis.
  - Cercles de portée, barres de vie, panneau HUD.

## Lancer le jeu

```bash
javac src/RedLightAlertGame.java
java -cp src RedLightAlertGame
```


## Affichage dans une page HTML

Une vue web est fournie pour afficher le jeu Java dans un navigateur:

```bash
python -m http.server
# puis ouvrir http://localhost:8000/web/
```

Cette vue HTML/Canvas reprend les mêmes éléments que la version Swing (emoji, grille, DLC, escarmouche, économie) pour une consultation sans environnement graphique Java.

## Détails de gameplay

1. **Économie**: chaque bâtiment génère un revenu ajouté à son camp à chaque cycle.
2. **Initiative**: phase alternée Alliés/Soviétiques; le compteur de tour augmente après cycle complet.
3. **Combat**: si une cible ennemie est en portée, l'unité inflige des dégâts.
4. **Destruction**: une unité à 0 PV est retirée de la carte.
5. **Progression DLC**: les unités/bâtiments sont filtrés par DLC activés dans la configuration.

## Extension recommandée

- Ajouter un vrai pathfinding (A*), brouillard de guerre et production active d'unités.
