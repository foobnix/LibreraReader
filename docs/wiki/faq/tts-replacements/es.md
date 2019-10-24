---
layout: main
---

# reemplazos TTS

> Los reemplazos se usan para cambiar la pronunciación de algunas palabras, para eliminar texto no deseado, para establecer la marca de estrés correcta

* Habilitar reemplazos TTS
* Mostrar el texto resultante de los reemplazos
* Diálogo de reemplazos

|1|2|3|
|-|-|-|
|![](1.png)|![](2.png)|![](3.png)|

Los reemplazos admiten el reemplazo clásico de una cadena a otra o puede usar expresiones RegExp

## Expresión

* &quot;texto&quot; - Texto simple
* &quot;* texto&quot; - * regla RexExp
* &quot;# texto&quot; - # regla deshabilitada
* &quot;text256&quot; - regla deshabilitada

## Ejemplos

* &quot;Lib&quot;. -&gt; &quot;Librera&quot; - reemplaza Lib. a Librera
* &quot;Librera&quot; -&gt; &quot;Libréra&quot; - agregue la marca de tensión correcta
* &quot;# Lib&quot;. -&gt; &quot;Librera&quot; - &quot;#&quot; para deshabilitar la regla
* &quot;* (L | l) ib.&quot; -&gt; &quot;$ 1ibrera&quot; - Reemplazar Lib. a Librera y lib. a librera
* &quot;* [()&quot; «» * ”“/[] &quot;-&gt;&quot; &quot;- Reemplazar caracteres por caracteres vacíos
* &quot;* [?!:; - | - | -]&quot; -&gt; &quot;,&quot; - Reemplazar caracteres para pausar (,) char

## Agregar archivo de regla

Librera admite archivos de reglas Regexp de @Voice reader
aquí hay algunos samle **demo-replaceces.txt**

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

   
