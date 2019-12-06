| <?php
/*
(C) 2005 Vidar Løkken <vidarlo@vestdata.no>

V.3: I've added escapeshellcmd to all user input that shows up directly
in exec()
*/
switch ($_REQUEST['output']) {
case "PostScript":
   $output=escapeshellcmd("-p $_REQUEST[paper]");
   break;
case "PDF":
   $output=escapeshellcmd("-a $_REQUEST[paper]");
   $pdf=1;
   break;
case "InLine":
   $output="-t";
   break;
}
if (isset($_FILES['userfile']['name'])) {
  $uploaddir = '/tmp/';
  $uploadfile = $uploaddir . $_FILES['userfile']['name'];
  $userfile =  $_FILES['userfile']['name'];
  if (move_uploaded_file($_FILES['userfile']['tmp_name'],$uploadfile)) {
    $delims=".";
    if (strstr($output,"-p")) {
      $psfile=strtok($userfile,$delims).".ps";
      header("Content-Type: Application/PostScript");
      header("Content-Disposition: attachment; filename=".$psfile);
      $file=escapeshellcmd($uploadfile);
      $command="antiword $output $file";
      passthru($command);
      unlink($uploadfile);
    } elseif (strstr($output,"-a")) {
      $psfile=strtok($userfile,$delims).".pdf";
      header("Content-Type: Application/PDF");
      //      header("Content-Disposition: attachment; filename=".$psfile);
      //      $command="antiword $output $uploadfile";
      $file=escapeshellcmd($uploadfile);
      $command="antiword $output $file";
      passthru($command);
      unlink($uploadfile);
    } else {
      echo "<pre>";
      $file=escapeshellcmd($uploadfile);
      $command="antiword $output $file";
      //      echo $command;
      //      $command="antiword $output $uploadfile";
      passthru($command);
      unlink($uploadfile);
    }
  }
  elseif (isset($_REQUEST['url'])) {
    echo $command;
    $url=$_REQUEST['url'];
    $uri=escapeshellcmd($_REQUEST['url']);
    $delim="/";
    $docfile=explode($delim,$uri);
    exec("wget -O /tmp/$docfile $url");
    if (strstr($output,"-p")) {
      $psfile=strtok(end($docfile),".").".ps";
      $safe=escapeshellcmd($docfile);
      $command="antiword $output /tmp/$safe";
      header("Content-Type: Application/PostScript");
      header("Content-Disposition: attachment; filename=".$psfile);
      passthru($command);
      @@      unlink("/tmp/$docfile");
    } elseif (strstr($output,"-a")) {
      $psfile=strtok(end($docfile),".").".pdf";
      $safe=escapeshellcmd($docfile);
      $command="antiword $output /tmp/$safe";
      header("Content-Type: Application/PDF");
      header("Content-Disposition: attachment; filename=".$psfile);
      passthru($command);
@@      unlink("/tmp/$docfile");
    } else {
      echo "<pre>";
      $safe=escapeshellcmd($docfile);
      $command="antiword $output /tmp/$safe";
      passthru($command);
@@      unlink("/tmp/$docfile");
    }
  }
}
if (!isset($_FILES['userfile']['name'])) {
  ?>
<p>
   This script converts a word file (most versions supported) into a
pure ASCII, a PDF or a PostScript version. Currently, only PostScript
and PDF carry images, and those images might be distorted or such. It's
based on the nice program antiword. see <a
href=http://antiword.cjb.net>antiword.cjb.net</a> for more information
about antiword. Currently, max file size is 3MiB for the upload. This
should be enough!
</p><p>Currently, I tend to end up with the ascii version being 1/100th
of the word document, and the pdf/ps versions being 1/10th of the size.
So if you're gonna send me a word document, rethink that. I'll not read
it. I'll read ascii, and probably pdf/ps too.</p>
</p>
<form enctype="multipart/form-data" action="antiword.php" method="post">
  <input type="hidden" name="MAX_FILE_SIZE" value="30000" />
  URL:<br /><input type="text" name="url" size=50 /><br />
  Send this file:<br /> <input name="userfile" type="file"/>
  <br />Output: <br />
  <SELECT name="output">
  <OPTGROUP>
  <OPTION name=txt>InLine</OPTION>
  <OPTION name=ps>PostScript</OPTION>
  <OPTION name=PDF>PDF</OPTION>
  </OPTGROUP>
  </SELECT>
Papersize: <SELECT name="paper"/>
<OPTGROUP>
<OPTION>a4</OPTION>
<OPTION>a3</OPTION>
<OPTION>a5</OPTION>
<OPTION>b4</OPTION>
<OPTION>b5</OPTION>
<OPTION>10x14</OPTION>
<OPTION>executive</OPTION>
<OPTION>folio</OPTION>
<OPTION>legal</OPTION>
<OPTION>letter</OPTION>
<OPTION>note</OPTION>
<OPTION>quarto</OPTION>
<OPTION>statement</OPTION>
<OPTION>tabloid</OPTION>
</select>
<br />
  <input type="submit" value="Send File" />
  </form>
<p>This is running <a href="http://antiword.cjb.net">antiword</a> 0.36. <br>
   Please drop me a note at antiword (at) bitsex.net if you have
comments for this.
<hr>
<font size=-1>(C)Vidar L&oslash;kken 2005</font>
<!-- Version: 0.2 as of 19. oct. 2005 -->
<?php
}
?>
|
