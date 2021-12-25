---
layout: main
---

# Data Backup and Migration

> Data backup is needed if you intend to transfer books to a new device, new folder, or SD card

# Export (Backup)

Tap _Export_ to save all application settings to a .zip file. Choose the folder to save your .zip file to and rename the file, if you wish.

Thus you will save:

* Application settings
* Bookmarks
* Reading progress
* User Tags
 
# Import

Tap _Import_ and find the .zip file with your backup data. Tap on the file and then tap _SELECT_

# Migrate

Migration will only replace the file paths in the App's config-files.

The full path is stored in Settings. For example, if the path to your book (example.pdf) is as follows:

/storage/Books/example.pdf

and you want to move it to the **MyBooks** folder, you need to change the location in the App's configuration file to:

/storage/MyBooks/example.pdf

Run _Migrate_, and replace:

Old path: **/Books/**  
New path: **/MyBooks/**

Tap _START MIGRATION_

If you're moving your book to an **external SD Card**, you can do it easily by replacing the destination:

_Migrate_: /storage/AAAA-AAAA/Books  to /storage/BBBB-BBBB/Books:

Old path: **/storage/AAAA-AAAA/**  
New path: **/storage/BBBB-BBBB/**

> **Reminder**: Don't forget to do _Export_ first to have a backup.

|1|2|3|
|-|-|-|
|![](1.png)|![](2.png)|![](3.png)|
