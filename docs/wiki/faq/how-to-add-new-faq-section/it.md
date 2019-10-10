---
layout: main
version: 5
---
[<](/wiki/faq)

# Come aggiungere una nuova sezione FAQ

Se vuoi aggiungere una nuova sezione FAQ, crea una cartella **[Title-sample-FAQ]** con solo il file inglese **index.md** qui.
[Https://github.com/foobnix/LibreraReader/tree/master/docs/wiki/faq](https://github.com/foobnix/LibreraReader/tree/master/docs/wiki/faq)

Quindi lo script aggiorna automaticamente il sommario delle faq e aggiunge traduzioni per tutte le lingue

Formato dell'intestazione del file: **index.md**

```
---
layout: main
version: 1
---
[<](/wiki/faq)
---

# Title sample FAQ

```

Tutte le immagini correlate per questa sezione dovrebbero essere nella cartella
```

* Desctiption 1
...
* Desctiption n

||||
|-|-|-|
|![](1.jpg)|![](2.jpg)|![](3.jpg)|

```

Se aggiorni la sezione FAQ dopo aver generato, dovresti aumentare di +1 la versione {3}:** nell'intestazione
```
---
layout: main
version: 2
---
```
