---
layout: main
---

# TTS-Ersatz

> Text-zu-Sprache-Ersetzungen werden verwendet, um die Art und Weise zu ändern, in der die Suchmaschine bestimmte Wörter ausspricht, bestimmte Zeichen beim Lesen zu überspringen oder korrekte Betonungsmarkierungen festzulegen.

* TTS-Ersetzungen aktivieren
* Zeigen Sie die Passage mit den Ersatzergebnissen an
* Das Dialogfeld **Ersetzungen** zum Festlegen von Ersetzungsregeln

|1|2|3|
|-|-|-|
|![](1.png)|![](2.png)|![](3.png)|

Klassische Ersetzungen werden unterstützt (einfaches Ändern einer Zeichenfolge durch eine andere) oder Sie können reguläre Ausdrücke (RegExp) verwenden.

## Ausdrücke

* &quot;text&quot; - Einfacher Text
* &quot;* text&quot; - * RexExp-Regel
* &quot;# text&quot; - deaktivierte Regel
* &quot;text256&quot; - deaktivierte Regel

## Beispiele

* &quot;Lib.&quot; -&gt; &quot;Librera&quot; - Lib ersetzen. mit Librera
* &quot;Librera&quot; -&gt; &quot;Libréra&quot; - Fügen Sie eine korrekte Belastungsmarke hinzu
* &quot;# Lib.&quot; -&gt; &quot;Librera&quot; - Benutze &quot;#&quot; um eine Regel zu deaktivieren
* * (L | l) ib. -&gt; &quot;$ 1ibrera&quot; - Lib ersetzen. mit Librera und lib. mit Librera
* &quot;* [()&quot; «» * ”“/[] &quot;-&gt;&quot; &quot;- Zeichen überspringen
* &quot;* [?!:; - | - | -]&quot; -&gt; &quot;,&quot; - Zeichen durch eine Pause ersetzen (,)

## TTS-Befehle

* &quot;text&quot; -&gt; ttsPAUSE - Pause nach &quot;text&quot; hinzufügen
* &quot;text&quot; -&gt; ttsSTOP - Stoppt TTS, wenn &quot;text&quot; im Satz gefunden wird
* &quot;text&quot; -&gt; ttsNEXT - Wechselt zur nächsten Seite, wenn der Satz &quot;text&quot; enthält
* &quot;text&quot; -&gt; ttsSKIP - Lesesatz überspringen, wenn &quot;text&quot; im Satz gefunden wird

## Fügen Sie eine Regeldatei hinzu

**Librera** unterstützt RegExp-Regeldateien von **@Voice Reader**.
Schauen Sie sich das folgende Beispiel **demo-replaces.txt** an:

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
## Ausgeschnittene Bereiche in PDF-Dokumenten überspringen
> Sehr häufig enthalten Seiten in PDF-Dateien (Bücher, Zeitschriftenartikel, Lehrbücher usw.) Kopf- und Fußzeilen, die sich über das gesamte Dokument erstrecken. Sie können die auslaufenden Köpfe mit zwei Fingern zuschneiden, um zur nächsten (und vorherigen) Seite zu gelangen. Ihre TTS-Engine hat jedoch keine Ahnung von Ihren Manipulationen. Sie müssen also angeben, was zu tun ist (überspringen Sie die nervige Sache!), Während Sie das Dokument vorlesen.

In **Librera** haben wir spezielle Ersetzungen (Befehle) eingeführt, mit denen Sie zugeschnittene Bereiche ignorieren und ein kontinuierliches, ununterbrochenes Lesen sicherstellen können.
* Geben Sie im Fenster **Ersetzungen** ein Wort oder eine Wortfolge in die linke Spalte ein und _ttsSKIP_ als Ersatz. Durch diesen Ersatz wird die Engine angewiesen, den Satz mit diesem Wort/dieser Wortfolge zu überspringen
* Geben Sie ein Wort oder eine Wortfolge in die linke Spalte ein und _ttsNEXT_ als Ersatz. Der Ersatz weist die Engine an, den Satz mit diesem Wort/dieser Wortfolge zu überspringen und sofort zur nächsten Seite zu wechseln
* Vergessen Sie nicht, _APPLY_ zu drücken, damit die Ersetzungen beibehalten werden

|4|5|
|-|-|
|![](4.png)|![](5.png)|

> **Testen Sie Ihre Änderungen einige Male, um sicherzustellen, dass alles ordnungsgemäß funktioniert.**
