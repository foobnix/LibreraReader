---
layout: main
---

{1}如何添加新的常见问题解答部分

如果要添加新的“常见问题解答”部分，请在此处创建仅包含英语**index.md**文件的文件夹**[example-faq-title]**。
[https://github.com/foobnix/LibreraReader/tree/master/docs/wiki/faq](https://github.com/foobnix/LibreraReader/tree/master/docs/wiki/faq)

然后，脚本会自动更新常见问题目录并添加所有语言的翻译

文件头格式：**index.md**

```
---
layout: main
---

# Example of Title
```

本节的所有相关图片都应该在文件夹中

```
* Image description 1
* Image description 2
* Image description 3

||||
|-|-|-|
|![](1.jpg)|![](2.jpg)|![](3.jpg)|
```
