---
layout: main
version: 4
---
[<](/wiki/faq)

# How to add new FAQ section

If you want to add a new FAQ section, create a folder **[Title-sample-FAQ]** with only the English **index.md** file here.
Then the script automatically updates the faq table of contents and adds translations for all languages

File header format: **index.md**

```
---
layout: main
version: 1
---
[<](/wiki/faq)
---

# Title sample FAQ

```

All related images for this section should be in the folder
```

* Desctiption 1
...
* Desctiption n

||||
|-|-|-|
|![](1.jpg)|![](2.jpg)|![](3.jpg)|

```

If you update the FAQ section after generatoin you should increase +1 the **version:** in the header 
```
---
layout: main
version: 2
---
```