---
layout: main
---

# كيفية إضافة قسم الأسئلة الشائعة الجديد

إذا كنت تريد إضافة قسم أسئلة وأجوبة جديد ، فقم بإنشاء مجلد **[example-faq-title]** مع ملف {{}} index.md** باللغة الإنجليزية فقط هنا.
[https://github.com/foobnix/LibreraReader/tree/master/docs/wiki/faq](https://github.com/foobnix/LibreraReader/tree/master/docs/wiki/faq)

ثم يقوم البرنامج النصي تلقائيًا بتحديث جدول المحتويات faq ويضيف ترجمات لجميع اللغات

تنسيق رأس الملف: **index.md**

```
---
layout: main
---

# Example of Title
```

يجب أن تكون جميع الصور المتعلقة بهذا القسم في المجلد

```
* Image description 1
* Image description 2
* Image description 3

||||
|-|-|-|
|![](1.jpg)|![](2.jpg)|![](3.jpg)|
```
