---
layout: main
---

# How to add new FAQ section

If you want to add a new FAQ section, create a folder **[example-faq-title]** with only the English **index.md** file here.
[https://github.com/foobnix/LibreraReader/tree/master/docs/wiki/faq](https://github.com/foobnix/LibreraReader/tree/master/docs/wiki/faq)

Then the script automatically updates the faq table of contents and adds translations for all languages

File header format: **index.md**

```
---
layout: main
---

# Example of Title
```

All related images for this section should be in the folder

```
* Image description 1
* Image description 2
* Image description 3

||||
|-|-|-|
|![](1.jpg)|![](2.jpg)|![](3.jpg)|
```