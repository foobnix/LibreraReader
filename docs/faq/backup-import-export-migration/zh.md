---
layout: main
---

#数据备份和迁移

>如果需要将书籍转移到新设备或新文件夹或新SD卡，则需要数据备份

#导出(备份)

按导出按钮将所有应用程序设置保存为.zip文件

导出保存：

*应用程序设置
*书签
*阅读进度
*用户标签
 
#导入
按导入以从.zip文件还原备份
如有必要，开始迁移

#迁移

迁移仅替换应用程序配置文件中的文件路径。

完整的书本路径存储在设置中，例如，如果您的书本放置在文件夹中

/storage/Books/example.pdf

然后将书移动到文件夹**MyBooks**

您需要在应用配置中设置新书的位置

/storage/MyBooks/example.pdf

您应该运行“迁移”并替换：

旧路径：**/书籍/**
新路径：**/ MyBooks /**


如果您将书籍放在**外部SD卡**上，则很容易为新的地方修复路径
迁移：/ storage/AAAA-AAAA/Books到/ storage/BBBB-BBBB/Books

旧路径：**/ storage/AAAA-AAAA /**
新路径：**/ storage/BBBB-BBBB /**

 
 

|1|2|3|
|-|-|-|
|![](1.png)|![](2.png)|![](3.png)|
