---
layout: main
---

# So fügen Sie ein neues FAQ-Thema hinzu

Wenn Sie ein neues FAQ-Thema hinzufügen möchten, erstellen Sie in [hier](https : //github.com/foobnix/LibreraReader/tree/master/docs/faq).

Das Skript aktualisiert automatisch das FAQ-Inhaltsverzeichnis und fügt Ländereinstellungsdateien für alle unterstützten Sprachen hinzu.

Dateikopfzeilenformat für **index.md**:

```
---
layout: main
---

# Topic Name from Here Goes to the FAQ Page
```

Sie können Ihre Diskussion mit Bildern (JPEG) veranschaulichen. Alle zu diesem Thema gehörenden Bilddateien sollten im Ordner neben **index.md** abgelegt werden.

```
* Image description 1
* Image description 2
* Image description 3

||||
|-|-|-|
|![](1.jpg)|![](2.jpg)|![](3.jpg)|
```
