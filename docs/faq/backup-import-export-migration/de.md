---
layout: main
---

# Datensicherung und -migration

> Datensicherung ist erforderlich, wenn Sie Bücher auf ein neues Gerät, einen neuen Ordner oder eine SD-Karte übertragen möchten

# Exportieren (Backup)

Tippen Sie auf _Exportieren_, um alle Anwendungseinstellungen in einer ZIP-Datei zu speichern. Wählen Sie den Ordner aus, in dem Sie Ihre ZIP-Datei speichern möchten, und benennen Sie die Datei bei Bedarf um.

So sparen Sie:

* Anwendungseinstellungen
* Lesezeichen
* Lesefortschritt
* Benutzer-Tags
 
# Importieren

Tippen Sie auf _Importieren_ und suchen Sie die ZIP-Datei mit Ihren Sicherungsdaten. Tippen Sie auf die Datei und dann auf _SELECT_

# Migrieren

Die Migration ersetzt nur die Dateipfade in den Konfigurationsdateien der App.

Der vollständige Pfad wird in den Einstellungen gespeichert. Wenn der Pfad zu Ihrem Buch (example.pdf) beispielsweise wie folgt lautet:

/storage/Books/example.pdf

und Sie es in den Ordner **MyBooks** verschieben möchten, müssen Sie den Speicherort in der Konfigurationsdatei der App wie folgt ändern:

/storage/MyBooks/example.pdf

Führen Sie _Migrate_ aus und ersetzen Sie:

Alter Pfad: **/ Books /**
Neuer Pfad: **/ MyBooks /**

Tippen Sie auf _MIGRATION STARTEN_

Wenn Sie Ihr Buch auf eine **externe SD-Karte**verschieben, können Sie dies ganz einfach tun, indem Sie das Ziel ersetzen:

_Migrieren_: /Speicher/AAAA-AAAA/Bücher nach /Speicher/BBBB-BBBB/Bücher:

Alter Pfad: **/storage/AAAA-AAAA/**
Neuer Pfad: **/storage/BBBB-BBBB/**

> **Erinnerung**: Vergessen Sie nicht, zuerst _Export_ durchzuführen, um ein Backup zu erstellen.

|1|2|3|
|-|-|-|
|![](1.png)|![](2.png)|![](3.png)|
