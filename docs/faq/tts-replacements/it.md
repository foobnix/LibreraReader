---
layout: main
---

# Sostituzioni TTS

> I sostituti di sintesi vocale vengono utilizzati per modificare il modo in cui il motore pronuncia determinate parole, saltare determinati caratteri durante la lettura o impostare segni di stress corretti.

* Abilita sostituzioni TTS
* Mostra il passaggio con i risultati di sostituzione
* La finestra di dialogo **Sostituzioni** per l'impostazione delle regole di sostituzione

|1|2|3|
|-|-|-|
|![](1.png)|![](2.png)|![](3.png)|

Sono supportati i sostituti classici (cambio diretto di una stringa per un'altra) oppure è possibile utilizzare espressioni regolari (RegExp).

## Espressioni

* &quot;testo&quot;: testo semplice
* &quot;* text&quot; - * Regola RexExp
* &quot;# text&quot; - regola disabilitata
* &quot;text256&quot; - regola disabilitata

## Esempi

* &quot;Lib.&quot; -&gt; &quot;Librera&quot; - sostituisci Lib. con Librera
* &quot;Librera&quot; -&gt; &quot;Libréra&quot; - aggiunge un segno di stress corretto
* &quot;# Lib.&quot; -&gt; &quot;Librera&quot; - usa &quot;#&quot; per disabilitare una regola
* &quot;* (L | l) ib.&quot; -&gt; &quot;$ 1ibrera&quot; - sostituisci Lib. con Librera e lib. con librera
* &quot;* [()&quot; «» * ”“/[] &quot;-&gt;&quot; &quot;- salta i caratteri
* &quot;* [?!:; - | - | -]&quot; -&gt; &quot;,&quot; - sostituisci i caratteri con una pausa (,)

## Comandi TTS

* &quot;testo&quot; -&gt; ttsPAUSE - Aggiungi pausa dopo &quot;testo&quot;
* &quot;testo&quot; -&gt; ttsSTOP - Ferma TTS se trova &quot;testo&quot; nella frase
* &quot;testo&quot; -&gt; ttsNEXT - Vai alla pagina successiva se trova &quot;testo&quot; nella frase
* &quot;text&quot; -&gt; ttsSKIP - Salta la lettura della frase se trova &quot;text&quot; nella frase

## Aggiungi un file di regole

**Librera** supporta i file delle regole RegExp da **@Voice Reader**
Dai un'occhiata a questo esempio **demo-replaces.txt** di seguito:

```
" живого " "живо́ва"
" как глаза " " как глаза́ "
" мне глаза" " мне глаза́"
" наклоняющая головы" "наклоня́ющая го́ловы"
" никакого стрелка" "никако́во стрелка́"
" ПОЖАРОБЕЗОПАСНУЮ СРЕДУ" "пожарабезопа́сную среду́."
" Стрелки!" "Стрелки́!"
" стрелки?" "стрелки́?"
", все так," ", всё так,"
"Зачем, стрелок?" "Зачем, стрело́к?"
"стрелок?" "стрело́к?"
*"(?i)\b\Q душа в душу\E\b" "душа́ в ду́шу"
*"(?i)\b\Q подогнулись\E\b" "падагну́лись"
*"(?i)\b\Q стрелки почувствовали\E\b" "стрелки́ почувствовали"
*"(?i)\b\Q стрелки продолжили\E\b" "стрелки́ продолжили"
*"(?i)\b\Q стрелку из\E\b" "стрелку́ из"
*"(?i)\b\Q стрелок\E\b" "стрело́к"
*"(?i)\b\Q стрелы\E\b" "стре́лы"
*"(?i)\b\Q*\E\b" "сно́ска"
*"(?i)\b\Q1 курса\E\b" "1-го курса"
*"(?i)\b\Q171 группы\E\b" "171-ой группы"
*"(?i)\b\Q1977\E\b" "1977-ой"
*"(?i)\b\QAcapela\E\b" "Акапэ́'ла"
*"(?i)\b\QBIOS\E\b" "БИ́“О́С"

*"(^| )(Д|д)-р" " доктор"
"(^| )(Г|г)-н" " господин"
*"(\d+)\s?-\s?я\b(?# ""я"" на границе слова)" "$1-я "
```
## Salta le aree ritagliate nei documenti PDF
> Molto spesso le pagine dei file PDF (libri, articoli di riviste, libri di testo, ecc.) Hanno intestazioni e piè di pagina che si estendono su tutto il documento. Puoi ritagliare le testine correndo con un pizzico di due dita, che passerà alle pagine successive (e precedenti). Ma il tuo motore TTS non ha idea delle tue manipolazioni. Quindi, devi dirgli cosa fare (salta la cosa fastidiosa!) Mentre leggi il documento ad alta voce per te.

In **Librera** abbiamo introdotto speciali sostituzioni (comandi) che ti consentiranno di ignorare le aree ritagliate e garantire una lettura continua e ininterrotta.
* Nella finestra **Sostituzioni**, inserisci una parola o una sequenza di parole nella colonna di sinistra e _ttsSKIP_ come sostituzione. Questa sostituzione dirà al motore di saltare la frase con questa sequenza di parole/parole
* Inserisci una parola o una sequenza di parole nella colonna di sinistra e _ttsNEXT_ come sua sostituzione. Il sostituto dirà al motore di saltare la frase con questa sequenza di parole/parole e andare immediatamente alla pagina successiva
* Non dimenticare di premere _APPLY_ per mantenere i sostituti

|4|5|
|-|-|
|![](4.png)|![](5.png)|

> **Verifica alcune volte le modifiche per assicurarti che tutto funzioni come dovrebbe!**
