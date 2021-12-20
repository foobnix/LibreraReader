---
layout: main
---

# Datensicherung und Migration

> Datensicherung erforderlich, wenn Sie Bücher auf ein neues Gerät oder in einen neuen Ordner oder eine neue SD-Karte übertragen müssen

# Exportieren (Backup)

Drücken Sie die Export-Taste, um alle Anwendungseinstellungen in der ZIP-Datei zu speichern

Export speichert:

* Anwendungseinstellungen
* Lesezeichen
* Lesefortschritt
* Benutzer-Tags
 
# Importieren
Klicken Sie auf Importieren, um die Sicherung aus der ZIP-Datei wiederherzustellen
Starten Sie gegebenenfalls die Migration

# Migration

Die Migration ersetzt nur Dateipfade in App-Konfigurationsdateien.

Der vollständige Buchpfad wird in den Einstellungen gespeichert, z. B. wenn Ihre Bücher in einem Ordner abgelegt wurden

/storage/Books/example.pdf

und dann verschieben Sie das Buch in den Ordner **MyBooks**

Sie müssen den Speicherort für neue Bücher in der App-Konfiguration festlegen

/storage/MyBooks/example.pdf

Sie sollten &quot;Migration&quot; ausführen und ersetzen:

Alter Pfad: **/ Books /**
Neuer Pfad: **/ MyBooks /**


Wenn Sie Bücher auf eine **externe SD-Karte** legen, können Sie leicht Pfade für einen neuen Ort festlegen
Migration:/storage/AAAA-AAAA/Bücher nach/storage/BBBB-BBBB/Books

alter Pfad: **/ storage/AAAA-AAAA /**
neuer Pfad: **/ storage/BBBB-BBBB /**

 
 

|1|2|3|
|-|-|-|
|![](1.png)|![](2.png)|![](3.png)|
