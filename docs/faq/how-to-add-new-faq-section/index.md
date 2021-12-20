---
layout: main
---

# How to Add a New FAQ Topic

If you want to add a new FAQ topic, create a folder **[whatever-hyphen-separated-name]** with a single en-us **index.md** file in [here](https://github.com/foobnix/LibreraReader/tree/master/docs/faq).

The script will automatically update the FAQ table of contents and add locale files for all supported languages.

File header format for **index.md**:

```
---
layout: main
---

# Topic Name from Here Goes to the FAQ Page
```

You can illustrate your discussion with pictures (JPEG). All image files related to this topic should be placed in the folder, alongside **index.md**

```
* Image description 1
* Image description 2
* Image description 3

||||
|-|-|-|
|![](1.jpg)|![](2.jpg)|![](3.jpg)|
```
