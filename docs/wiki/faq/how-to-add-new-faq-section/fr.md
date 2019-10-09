---
layout: main
version: 4
---
[<](/wiki/faq)

# Comment ajouter une nouvelle section FAQ

Si vous souhaitez ajouter une nouvelle section FAQ, créez un dossier **[Title-sample-FAQ]** contenant uniquement le fichier anglais **index.md**.
Ensuite, le script met automatiquement à jour la table des matières de la FAQ et ajoute des traductions pour toutes les langues.

Format d'en-tête de fichier:
**index.md**
```
---
layout: main
version: 1
---
[<](/wiki/faq)
---

# Title sample FAQ

```

Toutes les images liées à cette section doivent être dans le dossier
```

* Desctiption 1
...
* Desctiption n

||||
|-|-|-|
|![](1.jpg)|![](2.jpg)|![](3.jpg)|

```

Si vous mettez à jour la section FAQ après generatoin, vous devez augmenter de +1 la version {3}:** de l'en-tête.
```
---
layout: main
version: 2
---
```
