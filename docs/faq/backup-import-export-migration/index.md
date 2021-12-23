---
layout: main
---

# Data Backup and Migration

> Data backup is needed if you intend to transfer books to a new device, new folder, or sd card

# Export (Backup)

Press the Export button to save all application settings to a .zip file

Thus you will save:

* Application settings
* Bookmarks
* Reading progress
* User Tags
 
# Import

Press the Import button to restore your backup from a .zip file
Or you can initiate migration

# Migration

Migration will only replace the file paths in the App's config-files.

The full path is stored in Settings. For example, if the path to your book (example.pdf) is as follows:

/storage/Books/example.pdf

and you want to move it to the **MyBooks** folder,

you need to change the location in the App's configuration file to:

/storage/MyBooks/example.pdf

Run "Migration", and replace:

Old path: **/Books/**  
New path: **/MyBooks/**


If you're moving your book to an **external SD Card**, you can  do it easily by replacing the destination:
Migration: /storage/AAAA-AAAA/Books  to /storage/BBBB-BBBB/Books

old path: **/storage/AAAA-AAAA/** 
new path: **/storage/BBBB-BBBB/**

 
 

|1|2|3|
|-|-|-|
|![](1.png)|![](2.png)|![](3.png)|
