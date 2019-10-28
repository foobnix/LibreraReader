---
layout: main
---

# Codificação CSS personalizada

> Para renderização de livros, o **Librera** geralmente usa os estilos do arquivo .css do livro e também aplica suas configurações na janela **Preferências**. Também pode usar um ou outro separadamente. Mas às vezes não é suficiente. Alguns livros têm um código CSS tão peculiar que você não tem outra opção a não ser editar seus arquivos .css para melhorar a legibilidade. O **Librera**, no entanto, oferece outra opção: adicione temporariamente o código CSS personalizado, facilmente removível, assim que terminar o livro desafiado.

Três modos **Estilos** são suportados:

1. Documento + definido pelo usuário (pega as coisas boas dos dois mundos)
2. Documento (usa apenas as configurações .css do livro)
3. Definido pelo usuário (usa apenas a configuração do usuário especificada nas guias da janela **Preferências**)

* O usuário pode alternar entre os modos por meio de uma lista suspensa chamada ao tocar no link ao lado de _Styles_.
* Toque no ícone ao lado da lista _Styles_ para abrir a janela **Código CSS personalizado** e vá em frente.

|1|2|3|
|-|-|-|
|![](1.png)|![](2.png)|![](3.png)|

Documento + modo definido pelo usuário é ativado por padrão

O exemplo da Fig. 3 é retirado da vida real.

{white-space: pre-line;}
Seqüências de espaço em branco são recolhidas. As linhas são quebradas com caracteres de nova linha, em <br> e conforme necessário para preencher caixas de linha.

{white-space: pre;}
Sequências de espaço em branco são preservadas. As linhas são quebradas apenas em caracteres de nova linha na origem e em <br> elementos.

extensão {exibição: bloco}
p&gt; extensão {display: inline}
Elimina linhas vazias muito irritantes entre as páginas (corrigindo falhas de muPDF).
