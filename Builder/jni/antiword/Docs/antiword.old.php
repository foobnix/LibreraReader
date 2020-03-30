From: Paul Southworth <pauls@etext.org>
Subject: antiword PHP script
Date: Thu, 24 Oct 2002 14:01:05 -0700 (PDT)

Please find attached a trivial example of using a web form to process an
uploaded Word doc to text using antiword.  Perhaps other antiword users
would find it useful.

--Paul

<?
/* antiword.php
   A PHP script to convert uploaded MS Word docs to text using antiword.
   This script is public domain, no copyright.
   September 11, 2002
   Paul Southworth
*/
function print_form() {
?>
<html><head><title>antiword</title></head><body>
<form method=post action=antiword.php enctype="multipart/form-data">
<input name=upload type=file>
<input type=submit name=submit value=convert>
</form>
</body></html>
<?
}
if ($_FILES['upload']) {
    header ("Content-type: text/plain");
    system("/usr/local/bin/antiword " . $_FILES['upload']['tmp_name']);
} else {
    print_form();
}
?>
