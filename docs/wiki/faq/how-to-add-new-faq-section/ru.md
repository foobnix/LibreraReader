---
layout: main
version: 5
---
[<](/wiki/faq)

# Как добавить новый раздел FAQ

Если вы хотите добавить новый раздел часто задаваемых вопросов, создайте папку **[Title-sample-FAQ]**, содержащую только файл **index.md** на английском языке.
[Https://github.com/foobnix/LibreraReader/tree/master/docs/wiki/faq](https://github.com/foobnix/LibreraReader/tree/master/docs/wiki/faq)

Затем скрипт автоматически обновляет оглавление faq и добавляет переводы для всех языков.

Формат заголовка файла: **index.md**

```
---
layout: main
version: 1
---
[<](/wiki/faq)
---

# Title sample FAQ

```

Все связанные изображения для этого раздела должны быть в папке
```

* Desctiption 1
...
* Desctiption n

||||
|-|-|-|
|![](1.jpg)|![](2.jpg)|![](3.jpg)|

```

Если вы обновите раздел часто задаваемых вопросов после генерации, вы должны увеличить +1 к **версии:** в заголовке
```
---
layout: main
version: 2
---
```
