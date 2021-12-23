---
layout: main
---

# Comment ajouter un nouveau sujet de FAQ

Si vous souhaitez ajouter un nouveau sujet de FAQ, créez un dossier **[nom-séparé-de-trait-d'un-trait-unique]** avec un seul fichier en-us **index.md** dans [ici](https : //github.com/foobnix/LibreraReader/tree/master/docs/faq).

Le script mettra automatiquement à jour la table des matières de la FAQ et ajoutera des fichiers de paramètres régionaux pour toutes les langues prises en charge.

Format d'en-tête de fichier pour **index.md**:

```
---
layout: main
---

# Topic Name from Here Goes to the FAQ Page
```

Vous pouvez illustrer votre discussion avec des images (JPEG). Tous les fichiers image liés à cette rubrique doivent être placés dans le dossier, à côté de **index.md**.

```
* Image description 1
* Image description 2
* Image description 3

||||
|-|-|-|
|![](1.jpg)|![](2.jpg)|![](3.jpg)|
```
