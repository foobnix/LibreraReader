---
layout: main
---

# 数据备份和迁移

> 要将书籍转移到新设备、新文件夹或 SD 卡，需要进行数据备份

# 导出(备份)

点击 _Export_ 将所有应用程序设置保存为 .zip 文件。如果您愿意，请选择要将 .zip 文件保存到的文件夹并重命名该文件。

因此，您将节省：

*应用程序设置
*书签
*阅读进度
*用户标签
 
#导入

点击 _Import_ 并找到包含备份数据的 .zip 文件。点击文件，然后点击 _SELECT_

# 迁移

迁移只会替换应用程序配置文件中的文件路径。

完整路径存储在设置中。例如，如果你的书(example.pdf)的路径如下：

/storage/Books/example.pdf

并且要将其移动到 **MyBooks** 文件夹，您需要将应用程序配置文件中的位置更改为：

/storage/MyBooks/example.pdf

运行 _Migrate_，并替换：

旧路径：**/书籍/**
新路径：**/ MyBooks /**

点按_开始迁移_

如果您要将图书移动到**外部 SD 卡**，您可以通过替换目的地轻松实现：

_迁移_：/storage/AAAA-AAAA/书籍到/storage/BBBB-BBBB/书籍：

旧路径：**/storage/AAAA-AAAA/**
新路径：**/storage/BBBB-BBBB/**

> **提醒**：不要忘记先_导出_进行备份。

|1|2|3|
|-|-|-|
|![](1.png)|![](2.png)|![](3.png)|
