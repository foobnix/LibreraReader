---
layout: main
---

# Comment ajouter une nouvelle section FAQ

Si vous souhaitez ajouter une nouvelle section FAQ, créez un dossier **[example-faq-title]** contenant uniquement le fichier anglais **index.md**.
[https://github.com/foobnix/LibreraReader/tree/master/docs/wiki/faq](https://github.com/foobnix/LibreraReader/tree/master/docs/wiki/faq)

Ensuite, le script met automatiquement à jour la table des matières de la FAQ et ajoute des traductions pour toutes les langues.

Format d'en-tête de fichier: **index.md**

```
---
layout: main
---

# Example of Title
```

Toutes les images liées à cette section doivent être dans le dossier

```
* Image description 1
* Image description 2
* Image description 3

||||
|-|-|-|
|![](1.jpg)|![](2.jpg)|![](3.jpg)|
```
