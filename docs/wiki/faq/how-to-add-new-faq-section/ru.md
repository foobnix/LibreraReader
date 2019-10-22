---
layout: main
version: 5
---
[<](/wiki/faq/ru)

# Как добавить новый раздел FAQ

Если вы хотите добавить новый раздел часто задаваемых вопросов, создайте папку **[example-faq-title]** только с английским файлом **index.md**.
[Https://github.com/foobnix/LibreraReader/tree/master/docs/wiki/faq](https://github.com/foobnix/LibreraReader/tree/master/docs/wiki/faq)

Затем скрипт автоматически обновляет оглавление faq и добавляет переводы для всех языков.

Формат заголовка файла: **index.md**

```
---
layout: main
---
[<](/wiki/faq)

# Example of Title
```


Все связанные изображения для этого раздела должны быть в папке

```
* Image description 1
* Image description 2
* Image description 3

||||
|-|-|-|
|![](1.jpg)|![](2.jpg)|![](3.jpg)|
```
