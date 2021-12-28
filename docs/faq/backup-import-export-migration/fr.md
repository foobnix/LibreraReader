---
layout: main
---

# Sauvegarde et migration des données

> Une sauvegarde des données est nécessaire si vous avez l'intention de transférer des livres vers un nouvel appareil, un nouveau dossier ou une carte SD

# Exporter (Sauvegarder)

Appuyez sur _Exporter_ pour enregistrer tous les paramètres de l'application dans un fichier .zip. Choisissez le dossier dans lequel enregistrer votre fichier .zip et renommez le fichier, si vous le souhaitez.

Ainsi vous économiserez :

* Paramètres d'application
* Signets
* Progression de la lecture
* Tags utilisateur
 
# Importer

Appuyez sur _Importer_ et recherchez le fichier .zip contenant vos données de sauvegarde. Appuyez sur le fichier, puis appuyez sur _SELECT_

# Migrer

La migration ne remplacera que les chemins de fichiers dans les fichiers de configuration de l'application.

Le chemin complet est stocké dans Paramètres. Par exemple, si le chemin d'accès à votre livre (exemple.pdf) est le suivant :

/storage/Books/example.pdf

et que vous souhaitez le déplacer vers le dossier **MyBooks**, vous devez modifier l'emplacement dans le fichier de configuration de l'application pour :

/storage/MyBooks/example.pdf

Exécutez _Migrate_ et remplacez :

Ancien chemin: **/ Livres /**
Nouveau chemin: **/ MyBooks /**

Appuyez sur _START MIGRATION_

Si vous déplacez votre livre vers une **carte SD externe**, vous pouvez le faire facilement en remplaçant la destination :

_Migrer_ : /stockage/AAAA-AAAA/Livres vers /stockage/BBBB-BBBB/Livres :

Ancien chemin : **/storage/AAAA-AAAA/**
Nouveau chemin : **/stockage/BBBB-BBBB/**

> **Rappel** : N'oubliez pas de faire d'abord _Exporter_ pour avoir une sauvegarde.

|1|2|3|
|-|-|-|
|![](1.png)|![](2.png)|![](3.png)|
