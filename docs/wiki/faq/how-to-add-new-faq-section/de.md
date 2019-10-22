---
layout: main
version: 5
---
[<](/wiki/faq/de)

# So fügen Sie einen neuen FAQ-Bereich hinzu

Wenn Sie einen neuen FAQ-Bereich hinzufügen möchten, erstellen Sie hier einen Ordner **[Beispiel-FAQ-Titel]** mit nur der englischen Datei **index.md**.
[https://github.com/foobnix/LibreraReader/tree/master/docs/wiki/faq)(https://github.com/foobnix/LibreraReader/tree/master/docs/wiki/faq)

Das Skript aktualisiert dann automatisch das FAQ-Inhaltsverzeichnis und fügt Übersetzungen für alle Sprachen hinzu

Dateikopfzeilenformat: **index.md**

```
---
layout: main
---
[<](/wiki/faq)

# Example of Title
```


Alle verwandten Bilder für diesen Abschnitt sollten sich im Ordner befinden

```
* Image description 1
* Image description 2
* Image description 3

||||
|-|-|-|
|![](1.jpg)|![](2.jpg)|![](3.jpg)|
```
