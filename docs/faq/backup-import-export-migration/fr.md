---
layout: main
---

# Sauvegarde et migration des données

> Besoin de sauvegarde de données si vous devez transférer des livres vers un nouvel appareil ou vers un nouveau dossier ou une nouvelle carte SD

# Exporter (sauvegarde)

Appuyez sur le bouton d'exportation pour enregistrer tous les paramètres de l'application dans un fichier .zip

L'exportation enregistre:

* Paramètres d'application
* Signets
* Progression de la lecture
* Tags utilisateur
 
# Importer
Appuyez sur importer pour restaurer la sauvegarde à partir du fichier .zip
Lancez la migration si nécessaire

# Migration

La migration ne remplace que les chemins d'accès aux fichiers dans les fichiers de configuration des applications.

Le chemin complet du livre est stocké dans les paramètres, par exemple si vos livres ont été placés dans un dossier

/storage/Books/example.pdf

puis vous déplacez le livre vers le dossier **MyBooks**

Vous devez définir un nouvel emplacement pour les livres dans la configuration de l'application

/storage/MyBooks/example.pdf

Vous devez exécuter &quot;Migration&quot; et remplacer:

Ancien chemin: **/ Livres /**
Nouveau chemin: **/ MyBooks /**


Si vous placez des livres sur une **carte SD externe**, il est facile de fixer des chemins pour un nouvel emplacement
Migration:/stockage/AAAA-AAAA/Livres vers/stockage/BBBB-BBBB/Livres

ancien chemin: **/ stockage/AAAA-AAAA /**
nouveau chemin: **/ stockage/BBBB-BBBB /**

 
 

|1|2|3|
|-|-|-|
|![](1.png)|![](2.png)|![](3.png)|
