---
layout: main
version: 2
---
[<](/wiki/faq)

# Sostituzioni TTS

> Le sostituzioni vengono utilizzate per modificare la pronuncia di alcune parole, per eliminare il testo indesiderato, per impostare il segno di stress corretto

* Abilita sostituzioni TTS
* Mostra il testo del risultato delle sostituzioni
* Finestra di dialogo Sostituzioni

|1|2|3|
|-|-|-|
|![](1.png)|![](2.png)|![](3.png)|


Il supporto per i sostituti classico sostituisce una stringa con un'altra oppure puoi usare espressioni RegExp

# # Espressione

* &quot;testo&quot;: testo semplice
* &quot;* text&quot; - * Regola RexExp
* &quot;# testo&quot; - # regola disabilitata
* &quot;text256&quot; - regola disabilitata

# # Esempi

* &quot;Lib.&quot; -&gt; &quot;Librera&quot; - sostituisci Lib. a Librera
* &quot;Librera&quot; -&gt; &quot;Libréra&quot; - aggiunge il segno di stress corretto
* &quot;# Lib.&quot; -&gt; &quot;Librera&quot; - &quot;#&quot; per disabilitare la regola
* &quot;* (L | l) ib.&quot; -&gt; &quot;$ 1ibrera&quot; - Sostituisci lib. a Librera e lib. alla librera
* &quot;* [()&quot; «» * ”“/[]] &quot;-&gt;&quot; &quot;- Sostituisci caratteri in caratteri vuoti
* &quot;* [?!:; - | - | -]&quot; -&gt; &quot;,&quot; - Sostituisci caratteri per mettere in pausa (,) carattere

# # Aggiungi file di regole

Supporto di Librera File di regole Regexp dal lettore @Voice
ecco alcuni samle **demo-replaces.txt**

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


   
