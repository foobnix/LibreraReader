---
layout: main
---
[<](/wiki/faq)

# substituições TTS

&gt; Substituições são usadas para alterar a pronúncia de algumas palavras, excluir texto indesejado e definir marca de estresse

* Ativar substituições TTS
* Mostrar o texto resultante das substituições
Caixa de diálogo * Substituições

|1|2|3|
|-|-|-|
|![](1.png)|![](2.png)|![](3.png)|


As substituições suportam a substituição clássica de uma sequência por outra ou é possível usar expressões RegExp

# # expressão

* &quot;texto&quot; - texto simples
* &quot;* texto&quot; - * regra RexExp
* &quot;# text&quot; - # regra desativada
* &quot;text256&quot; - regra desabilitada

# # exemplos

* &quot;Lib.&quot; -&gt; &quot;Librera&quot; - substitua Lib. para Librera
* &quot;Librera&quot; -&gt; &quot;Libréra&quot; - adicione marca de tensão correta
* &quot;# Lib.&quot; -&gt; &quot;Librera&quot; - &quot;#&quot; para desativar a regra
* &quot;* (L | l) ib.&quot; -&gt; &quot;$ 1ibrera&quot; - Substitua Lib. para Librera e lib. a librera
* &quot;* [()&quot; «» * ”“/[]] &quot;-&gt;&quot; &quot;- Substitua caracteres por caracteres vazios
* &quot;* [?!:; - | - | -]&quot; -&gt; &quot;,&quot; - Substitua chars para pausar (,) char

# # Adicionar arquivo de regras

O Librera suporta arquivos de regras Regexp do @Voice reader
aqui está uma amostra **demo-replaces.txt**

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


   
