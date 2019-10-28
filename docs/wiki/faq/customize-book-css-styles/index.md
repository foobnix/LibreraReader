---
layout: main
---

# Custom CSS Coding

> For book rendering, **Librera** usually takes the styles from the book's .css file and also applies your settings from the **Preferences** window. It can also make use of one or the other separately. But sometimes it's not enough. Some books have so peculiar CSS code that you have no other choice but editing their .css files to improve readability. **Librera**, however, gives you another option: temporarily add custom CSS  code easily removable once you're done with the challenged book.

Three __Styles__ modes are supported:

1. Document + User-defined (takes the good stuff from the two worlds)
2. Document (uses just the book's .css settings)
3. User-defined (uses only the user's setting specified in the tabs of the **Preferences** window)

* The user can switch between the modes via a dropdown list invoked on tapping on the link next to _Styles_.
* Tap on the icon next to the _Styles_ list to open the **Custom CSS Code** window and go at it.

|1|2|3|
|-|-|-|
|![](1.png)|![](2.png)|![](3.png)|

Document + User-defined mode is enabled by default

The example in Fig. 3 is taken from real life.

{white-space: pre-line;}
    Sequences of white space are collapsed. Lines are broken at newline characters, at <br>, and as necessary to fill line boxes.

{white-space: pre;}
     Sequences of white space are preserved. Lines are only broken at newline characters in the source and at <br> elements.

span{display:block}
p>span{display:inline}
    Eliminates very annoying empty lines between pages (rectifying muPDF flaws).
