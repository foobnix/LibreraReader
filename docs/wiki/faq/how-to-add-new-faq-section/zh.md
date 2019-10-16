---
layout: main
version: 5
---
[<](/wiki/faq)

{1}如何添加新的常见问题解答部分

如果要添加新的“常见问题解答”部分，请在此处创建仅包含英语**index.md**文件的文件夹**[标题样本常见问题解答]**。
[https://github.com/foobnix/LibreraReader/tree/master/docs/wiki/faq](https://github.com/foobnix/LibreraReader/tree/master/docs/wiki/faq)

然后，脚本会自动更新常见问题目录并添加所有语言的翻译

文件头格式：**index.md**

```
---
layout: main
version: 1
---
[<](/wiki/faq)
---

# Title sample FAQ

```

本节的所有相关图片都应该在文件夹中
```

* Desctiption 1
...
* Desctiption n

||||
|-|-|-|
|![](1.jpg)|![](2.jpg)|![](3.jpg)|

```

如果您在生成后更新了FAQ部分，则应在标题中增加{3}版本：{2}的+1
```
---
layout: main
version: 2
---
```
