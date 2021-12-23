---
layout: main
---

# Como adicionar um novo tópico de FAQ

Se você deseja adicionar um novo tópico de FAQ, crie uma pasta **[qualquer nome separado por hífen]** com um único arquivo en-us **index.md** em [aqui](https : //github.com/foobnix/LibreraReader/tree/master/docs/faq).

O script atualizará automaticamente o índice das perguntas frequentes e adicionará arquivos de localidade para todos os idiomas suportados.

Formato do cabeçalho do arquivo para **index.md**:

```
---
layout: main
---

# Topic Name from Here Goes to the FAQ Page
```

Você pode ilustrar sua discussão com imagens (JPEG). Todos os arquivos de imagem relacionados a este tópico devem ser colocados na pasta, ao lado de **index.md**

```
* Image description 1
* Image description 2
* Image description 3

||||
|-|-|-|
|![](1.jpg)|![](2.jpg)|![](3.jpg)|
```
