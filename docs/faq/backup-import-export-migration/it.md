---
layout: main
---

# Backup e migrazione dei dati

> Il backup dei dati è necessario se si intende trasferire i libri su un nuovo dispositivo, una nuova cartella o una scheda SD

# Esporta (Backup)

Tocca _Esporta_ per salvare tutte le impostazioni dell'applicazione in un file .zip. Scegli la cartella in cui salvare il file .zip e rinomina il file, se lo desideri.

Così risparmierai:

* Impostazioni dell'applicazione
* Segnalibri
* Progressi nella lettura
* Tag utente
 
# Importa

Tocca _Importa_ e trova il file .zip con i tuoi dati di backup. Tocca il file, quindi tocca _SELECT_

# Migrazione

La migrazione sostituirà solo i percorsi dei file nei file di configurazione dell'app.

Il percorso completo è memorizzato in Impostazioni. Ad esempio, se il percorso del tuo libro (esempio.pdf) è il seguente:

/storage/Books/example.pdf

e vuoi spostarlo nella cartella **MyBooks**, devi cambiare la posizione nel file di configurazione dell'app in:

/storage/MyBooks/example.pdf

Esegui _Migrazione_ e sostituisci:

Vecchio percorso: **/ Libri /**
Nuovo percorso: **/ MyBooks /**

Tocca _AVVIA MIGRAZIONE_

Se stai spostando il tuo libro su una **scheda SD esterna**, puoi farlo facilmente sostituendo la destinazione:

_Migrazione_: /storage/AAAA-AAAA/Libri in /storage/BBBB-BBBB/Libri:

Vecchio percorso: **/storage/AAAA-AAAA/**
Nuovo percorso: **/storage/BBBB-BBBB/**

> **Promemoria**: non dimenticare di eseguire prima _Esporta_ per avere un backup.

|1|2|3|
|-|-|-|
|![](1.png)|![](2.png)|![](3.png)|
