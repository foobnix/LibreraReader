---
layout: main
---
[<](/wiki/faq)

# TTS Replacements

> Replacements are used to change the pronunciation of some words, to delete unwanted text, to set correct stress mark 

* Enable TTS Replacements
* Show the replacements resulted text
* Replacements dialog

|1|2|3|
|-|-|-|
|![](1.png)|![](2.png)|![](3.png)|


Replacements support classic replace one string to other or you can use RegExp expressions  

## Expression

* "text" - Simple text
* "*text" - * RexExp rule
* "#text" - # disabled rule
* "text256" - disabled rule

## Examples

* "Lib." -> "Librera" - replace Lib. to Librera  
* "Librera" -> "Libréra" - add correct stress mark
* "#Lib." -> "Librera" - "#" to disable rule
* "*(L&#124;l)ib." -> "$1ibrera" - Replace Lib. to Librera and lib. to librera
* "*[()"«»*”“/[]]" -> "" - Replace chars to empty char
* "*[?!:;–|—|―]" -> "," - Replace chars to pause(,) char

## Add rule file

Librera support Regexp rule files from @Voice reader
here is some samle **demo-replaces.txt**

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


   