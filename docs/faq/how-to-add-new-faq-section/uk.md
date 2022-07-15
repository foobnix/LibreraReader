---
layout: main
---

# Як додати нову тему поширених запитань

Якщо ви хочете додати нову тему поширених запитань, створіть папку **[whatever-hyphen-separated-name]** з одним файлом en-us **index.md** у [тут](https ://github.com/foobnix/LibreraReader/tree/master/docs/faq).

Сценарій автоматично оновить зміст поширених запитань і додасть файли локалі для всіх підтримуваних мов.

Формат заголовка файлу для **index.md**:

```
---
layout: main
---

# Topic Name from Here Goes to the FAQ Page
```

Ви можете проілюструвати свою дискусію малюнками (JPEG). Усі файли зображень, пов’язані з цією темою, слід розмістити в папці поруч із **index.md**

```
* Image description 1
* Image description 2
* Image description 3

||||
|-|-|-|
|![](1.jpg)|![](2.jpg)|![](3.jpg)|
```
