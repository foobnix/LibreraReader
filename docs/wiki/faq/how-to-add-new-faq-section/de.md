---
layout: main
version: 5
---
[<](/wiki/faq)

# So fügen Sie einen neuen FAQ-Bereich hinzu

Wenn Sie einen neuen FAQ-Bereich hinzufügen möchten, erstellen Sie hier einen Ordner **[Title-sample-FAQ]** mit nur der englischen Datei **index.md**.
[https://github.com/foobnix/LibreraReader/tree/master/docs/wiki/faq)(https://github.com/foobnix/LibreraReader/tree/master/docs/wiki/faq)

Das Skript aktualisiert dann automatisch das FAQ-Inhaltsverzeichnis und fügt Übersetzungen für alle Sprachen hinzu

Dateikopfzeilenformat: **index.md**

```
---
layout: main
version: 1
---
[<](/wiki/faq)
---

# Title sample FAQ

```

Alle verwandten Bilder für diesen Abschnitt sollten sich im Ordner befinden
```

* Desctiption 1
...
* Desctiption n

||||
|-|-|-|
|![](1.jpg)|![](2.jpg)|![](3.jpg)|

```

Wenn Sie den FAQ-Bereich nach der Generierung aktualisieren, sollten Sie die **-Version:** in der Kopfzeile um +1 erhöhen
```
---
layout: main
version: 2
---
```
