---
layout: main
---

# Codifica CSS personalizzata

> Per il rendering del libro, **Librera** di solito prende gli stili dal file .css del libro e applica anche le impostazioni dalla finestra **Preferenze**. Può anche utilizzare l'uno o l'altro separatamente. Ma a volte non è abbastanza. Alcuni libri hanno un codice CSS così particolare che non hai altra scelta che modificare i loro file .css per migliorare la leggibilità. **Librera**, tuttavia, ti offre un'altra opzione: aggiungi temporaneamente un codice CSS personalizzato facilmente rimovibile una volta terminato il libro contestato.

Sono supportate tre modalità **stili**:

1. Documento + Definito dall'utente (prende le cose buone dai due mondi)
2. Documento (utilizza solo le impostazioni .css del libro)
3. Definito dall'utente (utilizza solo le impostazioni dell'utente specificate nelle schede della finestra **Preferenze**)

* L'utente può passare da una modalità all'altra tramite un elenco a discesa invocato toccando il collegamento accanto a _Styles_.
* Tocca l'icona accanto all'elenco _Styles_ per aprire la finestra **Codice CSS personalizzato** e procedere.

|1|2|3|
|-|-|-|
|![](1.png)|![](2.png)|![](3.png)|

Document + La modalità definita dall'utente è abilitata per impostazione predefinita

L'esempio in Fig. 3 è tratto dalla vita reale.

{white-space: pre-line;}
Le sequenze di spazi bianchi vengono compresse. Le linee sono spezzate ai caratteri di nuova riga, a <br> e, se necessario, per riempire le caselle di riga.

{white-space: pre;}
Le sequenze di spazi bianchi vengono conservate. Le linee sono spezzate solo ai caratteri di nuova riga nella sorgente e in <br> elementi.

arco {display: block}
p&gt; arco {display: inline}
Elimina le righe vuote molto fastidiose tra le pagine (correggendo i difetti muPDF).
