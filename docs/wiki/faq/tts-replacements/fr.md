---
layout: main
---
[<](/wiki/faq/fr)

# Remplacements TTS

> Les remplacements sont utilisés pour modifier la prononciation de certains mots, pour supprimer le texte indésirable, pour définir le point d'insertion correct.

* Activer les remplacements TTS
* Afficher le texte résultant des remplacements
* Dialogue de remplacement

|1|2|3|
|-|-|-|
|![](1.png)|![](2.png)|![](3.png)|


Les remplacements prennent en charge Classic, remplacez une chaîne par une autre ou vous pouvez utiliser des expressions RegExp.

## Expression

* &quot;texte&quot; - Texte simple
* &quot;* text&quot; - * règle RexExp
* &quot;# text&quot; - # règle désactivée
* &quot;text256&quot; - règle désactivée

## Exemples

* &quot;Lib.&quot; -&gt; &quot;Librera&quot; - remplace Lib. à Librera
* &quot;Librera&quot; -&gt; &quot;Libréra&quot; - ajoute une marque de contrainte correcte
* &quot;# Lib.&quot; -&gt; &quot;Librera&quot; - &quot;#&quot; pour désactiver la règle
* &quot;* (L | l) ib.&quot; -&gt; &quot;$ 1ibrera&quot; - Remplacer Lib. à Librera et lib. à librera
* &quot;* [()&quot; «» * &quot;&quot;/[] &quot;-&gt;&quot; &quot;- Remplace les caractères par un caractère vide
* &quot;* [?!:; - - | - | -]&quot; -&gt; &quot;,&quot; - Remplacer les caractères pour faire une pause (,) char

## Ajouter un fichier de règles

Librera prend en charge les fichiers de règles Regexp de @Voice reader
voici quelques samle **demo-replaces.txt**

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


   
