---
layout: main
---

# Data backup and migration

> Data backup need if you need to transfer books to new device or to new folder or new sd card

# Export (backup)

Press export button to save all application settings in .zip file

Export saves:

* Application settings
* Bookmarks
* Reading progress
* User Tags
 
# Import
Press import to restore backup from .zip file
Start the migration if necessary

# Migration

Migration do only replaces file paths in app config files.

Full book path is stored in settings, for example if your books were placed in folder

/storage/Books/example.pdf

and then you move book to the folder **MyBooks**

You need to set new books location in app configuration

/storage/MyBooks/example.pdf

You should run "Migration" and replace:

Old path: **/Books/**  
New path: **/MyBooks/**


If you place books on **external SD Card** it's easy to fix paths for new place
Migration: /storage/AAAA-AAAA/Books  to /storage/BBBB-BBBB/Books

old path: **/storage/AAAA-AAAA/** 
new path: **/storage/BBBB-BBBB/**

 
 

|1|2|3|
|-|-|-|
|![](1.png)|![](2.png)|![](3.png)|
