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
