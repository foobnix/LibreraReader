---
layout: main
version: 5
---
[<](/wiki/faq)

# Como adicionar uma nova seção de perguntas frequentes

Se você quiser adicionar uma nova seção de Perguntas frequentes, crie uma pasta **[FAQ-de-título-título]** com apenas o arquivo em inglês **index.md** aqui.
[https://github.com/foobnix/LibreraReader/tree/master/docs/wiki/faqorgeous(https://github.com/foobnix/LibreraReader/tree/master/docs/wiki/faq)

Em seguida, o script atualiza automaticamente o índice faq e adiciona traduções para todos os idiomas

Formato do cabeçalho do arquivo: **index.md**

```
---
layout: main
version: 1
---
[<](/wiki/faq)
---

# Title sample FAQ

```

Todas as imagens relacionadas a esta seção devem estar na pasta
```

* Desctiption 1
...
* Desctiption n

||||
|-|-|-|
|![](1.jpg)|![](2.jpg)|![](3.jpg)|

```

Se você atualizar a seção FAQ após gerar, você deve aumentar +1 na versão {3}:** no cabeçalho
```
---
layout: main
version: 2
---
```
