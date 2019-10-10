---
layout: main
version: 2
---
[<](/wiki/faq)

# TTS-Ersatz

> Ersetzungen werden verwendet, um die Aussprache einiger Wörter zu ändern, unerwünschten Text zu löschen und die richtige Betonung zu setzen

* TTS-Ersetzungen aktivieren
* Zeigen Sie den Ergebnistext der Ersetzungen an
* Dialogfeld &quot;Ersetzungen&quot;

|1|2|3|
|-|-|-|
|![](1.png)|![](2.png)|![](3.png)|


Ersetzungen unterstützen das klassische Ersetzen einer Zeichenfolge durch eine andere oder Sie können RegExp-Ausdrücke verwenden

# # Ausdruck

* &quot;text&quot; - Einfacher Text
* &quot;* text&quot; - * RexExp-Regel
* &quot;# text&quot; - # deaktivierte Regel
* &quot;text256&quot; - deaktivierte Regel

# # Beispiele

* &quot;Lib.&quot; -&gt; &quot;Librera&quot; - Lib ersetzen. nach Librera
* &quot;Librera&quot; -&gt; &quot;Libréra&quot; - korrekte Spannungsmarkierung hinzufügen
* &quot;# Lib.&quot; -&gt; &quot;Librera&quot; - &quot;#&quot;, um die Regel zu deaktivieren
* * (L | l) ib. -&gt; &quot;$ 1ibrera&quot; - Lib ersetzen. zu Librera und lib. zu librera
* &quot;* [()&quot; «» * ”“/[]] &quot;-&gt;&quot; &quot;- Ersetzen Sie die Zeichen durch leere Zeichen
* &quot;* [?!:; - | - | -]&quot; -&gt; &quot;,&quot; - Zeichen ersetzen, um (,) char anzuhalten

# # Regeldatei hinzufügen

Librera unterstützt Regexp-Regeldateien vom @ Voice Reader
Hier einige Beispiele **demo-replaces.txt**

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


   
