---
layout: main
---

# reemplazos TTS

> Los reemplazos de texto a voz se usan para cambiar la forma en que el motor pronuncia ciertas palabras, para omitir ciertos caracteres mientras lee o para establecer las marcas de estrés correctas.

* Habilitar reemplazos TTS
* Mostrar el pasaje con resultados de reemplazo
* El cuadro de diálogo **Reemplazos** para establecer reglas de reemplazo

|1|2|3|
|-|-|-|
|![](1.png)|![](2.png)|![](3.png)|

Se admiten reemplazos clásicos (cambio directo de una cadena por otra), o puede usar expresiones regulares (RegExp).

## Expresiones

* &quot;texto&quot; - Texto simple
* &quot;* texto&quot; - * regla RexExp
* &quot;# texto&quot; - regla deshabilitada
* &quot;text256&quot; - regla deshabilitada

## Ejemplos

* &quot;Lib&quot;. -&gt; &quot;Librera&quot; - reemplaza Lib. con librera
* &quot;Librera&quot; -&gt; &quot;Libréra&quot; - agregue una marca de tensión correcta
* &quot;# Lib&quot;. -&gt; &quot;Librera&quot; - usa &quot;#&quot; para deshabilitar una regla
* &quot;* (L | l) ib.&quot; -&gt; &quot;$ 1ibrera&quot; - reemplaza Lib. con Librera y lib. con librera
* &quot;* [()&quot; «» * ”“/[] &quot;-&gt;&quot; &quot;- saltar caracteres
* &quot;* [?!:; - | - | -]&quot; -&gt; &quot;,&quot; - reemplazar caracteres con una pausa (,)

## Comandos TTS

* &quot;texto&quot; -&gt; ttsPAUSE - Agregar pausa después de &quot;texto&quot;
* &quot;texto&quot; -&gt; ttsSTOP - Detiene TTS si encuentra &quot;texto&quot; en la oración
* &quot;texto&quot; -&gt; ttsSIGUIENTE - Ir a la página siguiente si encuentra &quot;texto&quot; en la oración
* &quot;texto&quot; -&gt; ttsSKIP - Omita la lectura de la oración si encuentra &quot;texto&quot; en la oración

## Agregar un archivo de reglas

**Librera** admite archivos de reglas RegExp de **@Voice Reader**
Consulte este ejemplo **demo-replaceces.txt** a continuación:

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
## Saltar áreas recortadas en documentos PDF
> Muy a menudo, las páginas en archivos PDF (libros, artículos de revistas, libros de texto, etc.) tienen encabezados y pies de página que se ejecutan en todo el documento. Puede recortar los cabezales con una pizca de dos dedos, que pasará a las páginas siguientes (y anteriores). Pero su motor TTS no tiene ni idea de sus manipulaciones. Por lo tanto, debe decirle qué hacer (¡omita lo molesto!) Mientras le lee el documento en voz alta.

En **Librera** hemos introducido reemplazos especiales (comandos) que le permitirán ignorar las áreas recortadas y garantizar una lectura continua e ininterrumpida.
* En la ventana **Reemplazos**, ingrese una palabra o secuencia de palabras en la columna izquierda y _ttsSKIP_ como su reemplazo. Este reemplazo le indicará al motor que omita la oración con esta palabra/secuencia de palabras
* Ingrese una palabra o secuencia de palabras en la columna izquierda y _ttsNEXT_ como su reemplazo. El reemplazo le indicará al motor que omita la oración con esta palabra/secuencia de palabras e inmediatamente vaya a la página siguiente
* No olvides presionar _APPLY_ para permitir que se mantengan los reemplazos

|4|5|
|-|-|
|![](4.png)|![](5.png)|

> **¡Pruebe sus cambios varias veces para asegurarse de que todo funcione como debería!**
