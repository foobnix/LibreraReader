;---------------------------------------------------------;
;Created 08-24 april 2013 by                                 ;
;(C) Kravtsov Konstantin Ivanovich, Novokuznetsk, Russia  ;
;State of Art edition version - see below NSI_VER                                   ;
;LogicLib and Winver applied by Leon Bottou               ;             ;
;----------------------------------------------------------

;Used software:
;--------------
;NSIS 3.05 (http://sourceforge.net/projects/nsis/files/)
;add-on NSIS 3.05+log (http://sourceforge.net/projects/nsis/files/NSIS%203/3.05/nsis-3.05-log.zip)
;HM NIS Edit 2.0.3 -- it's good IDE for NSI building
;
;Used docs
;---------
;NSIS docs and manuals at http://forum.oszone.net/thread-248731.html
;NSIS help at http://forum.oszone.net/thread-168287.html
;NSIS 2.46 User Manual shipped with NSIS




;INSTALLER START
;---------------
;prepare names

RequestExecutionLevel admin
Unicode true

!define NSI_VER "9.2"
!define DJVULIBRE_NAME "DjVuLibre"
!define DJVULIBRE_VERSION "3.5.28"
!define CLASSES "Software\Classes\"
!define DJVIEW_NAME "DjView"
!define DJVIEW_VERSION "4.12"
!define VI_PRODUCT_VERSION "4.12.0.0"

!define PRODUCT_NAME "${DJVULIBRE_NAME} ${DJVIEW_NAME}"
!define UNINST_NAME "${DJVULIBRE_NAME}+${DJVIEW_NAME}" ; for uninstaller
!define MENU_NAME "${DJVULIBRE_NAME}"
!define PRODUCT_VERSION "${DJVULIBRE_VERSION}+${DJVIEW_VERSION}"

;provide other info
!define PRODUCT_PUBLISHER "DjVuZone"
!define DJVULIBRE_WEB_SITE "http://djvu.sourceforge.net"
!define DJVUORG_WEB_SITE "http://djvu.org"
!define DJVIEW_OPTS "--outline --continuous"
!define PRODUCT_DOWNLOAD_PAGE "http://sourceforge.net/projects/djvu/files/DjVuLibre_Windows/"
!define PRODUCT_ONLINEHELP_PAGE "http://djvu.sourceforge.net/doc/index.html"
!define PRODUCT_DIR_REGKEY "Software\Microsoft\Windows\CurrentVersion\App Paths\djview.exe"
!define PRODUCT_UNINST_KEY "Software\Microsoft\Windows\CurrentVersion\Uninstall\${UNINST_NAME}"
!define PRODUCT_UNINST_ROOT_KEY "HKLM"


;label for HCR
!define FILEID "Djview.DjVuFile"
!define NO_EXT "no key"
!define BACKUP_EXT "DjView.Backup"

;const for url.lnk
!define RUN_URL "$WINDIR\system32\rundll32.exe"
!define ICO_URL "$WINDIR\system32\url.dll"
!define URL_PAR " url.dll,FileProtocolHandler"

;vars
Var Djvu_EXT ; ext var
Var COUNT    ; counter var
Var KEY_VAL  ; readed key valuse
Var TMP_EXT  ; for temp reading ext val from reg
Var INST_LOG_REN ; flag of exist install.log

;fill installer description
VIProductVersion "${VI_PRODUCT_VERSION}"

;end-user doesnot see
;djvulibre version anywhere except installer
;dll's and utils doesn't report any
VIAddVersionKey "DjvuLibreVersion" ${DJVULIBRE_VERSION}
VIAddVersionKey "DjViewVersion" ${DJVIEW_VERSION}
VIAddVersionKey "Installer by" "Konstantin Kravtsov (C) 2013"
VIAddVersionKey "FileVersion" ${PRODUCT_VERSION}
VIAddVersionKey "ProductName" "${PRODUCT_NAME}"
VIAddVersionKey "LegalCopyright" "GPL v2+"
VIAddVersionKey "CompanyName" ${PRODUCT_PUBLISHER}
VIAddVersionKey "FileDescription" "DjVu view, edit and create tools for Windows"

;----------------
;Includes section
;----------------

!include "LogicLib.nsh"
!include "WinVer.nsh"


;---------------
;Install section
;---------------

SetCompressor /SOLID lzma
Name "${PRODUCT_NAME} ${PRODUCT_VERSION}"
OutFile "..\DjVuLibre-${DJVULIBRE_VERSION}_DjView-${DJVIEW_VERSION}_Setup.exe"
InstallDir "$PROGRAMFILES\DjVuLibre"
InstallDirRegKey HKLM "${PRODUCT_DIR_REGKEY}" ""
ShowInstDetails show
ShowUnInstDetails show

; MUI 1.67 compatible ------
!include "MUI.nsh"

;settings
!define MUI_ABORTWARNING
!define MUI_HEADERIMAGE
!define MUI_ICON "${NSISDIR}\Contrib\Graphics\Icons\win-install.ico"
!define MUI_UNICON "${NSISDIR}\Contrib\Graphics\Icons\win-uninstall.ico"

;language selection dialog settings
!define MUI_LANGDLL_ALWAYSSHOW
!define MUI_LANGDLL_REGISTRY_ROOT "${PRODUCT_UNINST_ROOT_KEY}"
!define MUI_LANGDLL_REGISTRY_KEY "${PRODUCT_UNINST_KEY}"
!define MUI_LANGDLL_REGISTRY_VALUENAME "NSIS:Language"

;workaround lang change effect
!define MUI_PAGE_CUSTOMFUNCTION_PRE preWelcomePage

;set components decription layout bottom
!define MUI_COMPONENTSPAGE_SMALLDESC


;welcome page
!define MUI_WELCOMEPAGE_TITLE_3LINES
!insertmacro MUI_PAGE_WELCOME

;license page
!insertmacro MUI_PAGE_LICENSE "COPYING.txt"

;directory page
!insertmacro MUI_PAGE_DIRECTORY

;install options page
!insertmacro MUI_PAGE_COMPONENTS

;instfiles page
!insertmacro MUI_PAGE_INSTFILES

;finish page
!define MUI_FINISHPAGE_RUN " "
!define MUI_FINISHPAGE_RUN_TEXT $(Launch_LAB)
!define MUI_FINISHPAGE_RUN_NOTCHECKED
!define MUI_FINISHPAGE_RUN_FUNCTION "LaunchReadme"
!define MUI_FINISHPAGE_NOAUTOCLOSE
!define MUI_FINISHPAGE_TITLE_3LINES
!insertmacro MUI_PAGE_FINISH

;uninstaller pages
!insertmacro MUI_UNPAGE_INSTFILES

;reserve files
!insertmacro MUI_RESERVEFILE_INSTALLOPTIONS

;language files
!insertmacro MUI_LANGUAGE "English"
!insertmacro MUI_LANGUAGE "Russian"
!insertmacro MUI_LANGUAGE "Spanish"

; end MUI 1.67 compatible ------


Function .onInit
  ;check language
  !insertmacro MUI_LANGDLL_DISPLAY
FunctionEnd

Function preWelcomePage
  ReadRegStr $KEY_VAL  ${PRODUCT_UNINST_ROOT_KEY} "${PRODUCT_UNINST_KEY}" "UninstallString"
  ${If} "$KEY_VAL" != ""
    ReadRegStr $COUNT ${PRODUCT_UNINST_ROOT_KEY} "${PRODUCT_UNINST_KEY}" "DisplayVersion"
    ${If} "$COUNT" == "${PRODUCT_VERSION}"
      MessageBox MB_YESNO|MB_ICONEXCLAMATION $(Message_AlreadyInstalled) IDNO done
    ${ElseIf} "$COUNT" S< "${PRODUCT_VERSION}"
      MessageBox MB_YESNO|MB_ICONEXCLAMATION $(Message_OldFound) IDNO done
    ${ElseIf} "$COUNT" S> "${PRODUCT_VERSION}"
      MessageBox MB_YESNO|MB_ICONEXCLAMATION $(Message_NewFound) IDNO done
    ${EndIf}
    ;Run the uninstaller
    ClearErrors
    ExecWait '"$KEY_VAL" /S' ;; silent
  ${EndIf}
done:
FunctionEnd

Function RegisterExt
  ; Kravtsov Konstantin Ivanovich (C) 2013
  ; for register ext. Push EXT with ext and call
  Pop $Djvu_EXT
  ClearErrors
  ReadRegStr $TMP_EXT HKLM "${CLASSES}$Djvu_EXT" ""
  ${If} ${Errors}
    WriteRegStr HKLM "${CLASSES}$Djvu_EXT" ${BACKUP_EXT} "${NO_EXT}"
  ${ElseIf} "$TMP_EXT" != "${FILEID}"
    WriteRegStr HKLM "${CLASSES}$Djvu_EXT" "${BACKUP_EXT}" "$TMP_EXT"
  ${EndIf}
  WriteRegStr HKLM "${CLASSES}$Djvu_EXT" "" "${FILEID}"
FunctionEnd

Function LaunchReadme
  Exec '"$INSTDIR\djview.exe" --outline --continuous "$INSTDIR\doc\djvulibre-book-en.djvu"'
FunctionEnd


;---install files
Section "-!DjVuLibre" scDjVuLibre
  SectionIn RO
  SetOutPath "$INSTDIR"
  SetOverwrite try
  StrCpy $INST_LOG_REN 0 ;set var to zero
  StrCpy $TMP_EXT "$INSTDIR\install.log"
  ${If} ${FileExists} $TMP_EXT
    Rename $TMP_EXT $INSTDIR\install.kik
    StrCpy $INST_LOG_REN 1 ;set var to rename was
  ${EndIf}
  StrCpy $TMP_EXT "$INSTDIR\uninstall.${PRODUCT_VERSION}"
  ${If} ${FileExists} $TMP_EXT
    Rename $INSTDIR\uninstall.${PRODUCT_VERSION} $INSTDIR\install.log
  ${EndIf}
  ;; all files are installed now
  Logset on
  File "*.exe"
  File "*.dll"
  File "*.lib"
  File /r "include"
  File /r "doc"
  File /r "plugins"
  File /r "share"
  File "qt.conf"
  File "djvulibre*.ns?"
  File "COPYING.txt"
  Logset off
  ;; call vcredist
  ExecWait '"$INSTDIR/vcredist_x86.exe" /passive /norestart'
SectionEnd

;--- registry
Section "-registry"
  SectionIn RO
  DetailPrint "Updating registry"
  ;; app registration
  WriteRegStr HKLM "${PRODUCT_DIR_REGKEY}" "" "$INSTDIR\djview.exe"
  WriteRegStr HKLM "${PRODUCT_DIR_REGKEY}" "Path" "$INSTDIR"
  WriteRegStr HKLM "${CLASSES}Applications\djview.exe" "FriendlyAppName" "DjView"
  WriteRegStr HKLM "${CLASSES}Applications\djview.exe\SupportedTypes\.djvu" "" ""
  WriteRegStr HKLM "${CLASSES}Applications\djview.exe\SupportedTypes\.djv" "" ""
  WriteRegStr HKLM "${CLASSES}Applications\djview.exe\shell\open\command" "" '"$INSTDIR\djview.exe" "%1"'
  ${If} ${AtLeastWin7}
    WriteRegDWORD HKLM "${PRODUCT_DIR_REGKEY}" "UseUrl" 1
    WriteRegStr HKLM "${CLASSES}Applications\djview.exe\SupportedProtocols\http" "" ""
    WriteRegStr HKLM "${CLASSES}Applications\djview.exe\SupportedProtocols\https" "" ""
  ${EndIf}
  ;; fileid
  DeleteRegKey HKLM "${CLASSES}${FILEID}"
  WriteRegStr HKLM "${CLASSES}${FILEID}" "" "DjVu File"
  WriteRegStr HKLM "${CLASSES}${FILEID}\DefaultIcon" "" "$INSTDIR\djview.exe,1"
  WriteRegStr HKLM "${CLASSES}${FILEID}\shell\open\command" "" '"$INSTDIR\djview.exe" "%1"'
  ;; open with
  WriteRegStr HKLM "${CLASSES}.djv\OpenWithProgids" ${FILEID} ""
  WriteRegStr HKLM "${CLASSES}.djvu\OpenWithProgids" ${FILEID} ""
  ClearErrors
  ReadRegStr $TMP_EXT HKLM "${CLASSES}DjVu.Document" ""
  ${IfNot} ${Errors}
    WriteRegStr HKLM "${CLASSES}.djv\OpenWithProgids" "DjVu.Document" ""
    WriteRegStr HKLM "${CLASSES}.djvu\OpenWithProgids" "DjVu.Document" ""
  ${EndIf}
SectionEnd

;--- associations
Section "$(secAssoc)" scAssoc
  DetailPrint "Creating associations"
  push ".djvu"
  Call RegisterExt
  push ".djv"
  Call RegisterExt
SectionEnd

;--- shortcuts
Section "-menuentries"
  SectionIn RO
  DetailPrint "Creating menu entries"

  ;clear old menu
  rmdir /r "$SMPROGRAMS\${MENU_NAME}"

  ;; all menu entries
  LogSet on
  SetShellVarContext all
  CreateDirectory "$SMPROGRAMS\${MENU_NAME}"
  CreateShortCut "$SMPROGRAMS\${MENU_NAME}\DjView.lnk" "$INSTDIR\djview.exe"
  CreateDirectory "$SMPROGRAMS\${MENU_NAME}\$(Uninst_DIR)"
  CreateShortCut "$SMPROGRAMS\${MENU_NAME}\$(Uninst_DIR)\$(Uninst_LNK).lnk" "$INSTDIR\uninst.exe"
  CreateDirectory "$SMPROGRAMS\${MENU_NAME}\$(Doc_DIR)"
  CreateShortCut "$SMPROGRAMS\${MENU_NAME}\$(Doc_DIR)\$(WEB_LNK) Djvu.org.lnk" ${RUN_URL} "${URL_PAR} ${DJVUORG_WEB_SITE}" ${ICO_URL} 0
  CreateShortCut "$SMPROGRAMS\${MENU_NAME}\$(Doc_DIR)\$(WEB_LNK) DjvuLibre.lnk" ${RUN_URL} "${URL_PAR} ${DJVULIBRE_WEB_SITE}" ${ICO_URL} 0
  CreateShortCut "$SMPROGRAMS\${MENU_NAME}\$(Doc_DIR)\$(WEB_LNK) DjVuLibre $(WebDL_LNK).lnk" ${RUN_URL} "${URL_PAR} ${PRODUCT_DOWNLOAD_PAGE}" ${ICO_URL} 0
  CreateShortCut "$SMPROGRAMS\${MENU_NAME}\$(Doc_DIR)\Online documentation.lnk" ${RUN_URL} "${URL_PAR} ${PRODUCT_ONLINEHELP_PAGE}" ${ICO_URL} 0
  CreateShortCut "$SMPROGRAMS\${MENU_NAME}\$(Doc_DIR)\$(Lic_TXT).lnk" "$INSTDIR\COPYING.txt"
  CreateShortCut "$SMPROGRAMS\${MENU_NAME}\$(Doc_DIR)\DjVuLibre documentation.lnk" "$INSTDIR\djview.exe" '${DJVIEW_OPTS} "$INSTDIR\doc\djvulibre-book-en.djvu"'
  CreateShortCut "$SMPROGRAMS\${MENU_NAME}\$(Doc_DIR)\Specification of the DjVu format (v2).lnk" "$INSTDIR\djview.exe" '${DJVIEW_OPTS} "$INSTDIR\doc\djvu2spec.djvu"'
  CreateShortCut "$SMPROGRAMS\${MENU_NAME}\$(Doc_DIR)\Specification of the DjVu format (v3).lnk" "$INSTDIR\djview.exe" '${DJVIEW_OPTS} "$INSTDIR\doc\djvu3spec.djvu"'
  CreateShortCut "$SMPROGRAMS\${MENU_NAME}\$(Doc_DIR)\Specification of the DjVu format (changes).lnk" "$INSTDIR\doc\djvuchanges.txt"
  CreateShortCut "$SMPROGRAMS\${MENU_NAME}\$(Doc_DIR)\Lizardtech License Notes 2002.lnk" "$INSTDIR\djview.exe" '"$INSTDIR\doc\lizard2002.djvu"'
  CreateShortCut "$SMPROGRAMS\${MENU_NAME}\$(Doc_DIR)\Lizardtech License Notes 2007.lnk" "$INSTDIR\djview.exe" '"$INSTDIR\doc\lizard2007.djvu"'
  ${If} $Language == ${LANG_RUSSIAN}
    CreateShortCut "$SMPROGRAMS\${MENU_NAME}\$(Doc_DIR)\$(Doc_LNK) ${DJVULIBRE_NAME}.lnk" "$INSTDIR\djview.exe" '${DJVIEW_OPTS} "$INSTDIR\doc\djvulibre-book-ru.djvu"'
  ${EndIf}
  LogSet off
SectionEnd

Section "$(secDesk)" scDesk
  DetailPrint "Creating desktop shortcut"
  SetShellVarContext all
  LogSet on
  CreateShortCut "$DESKTOP\DjView.lnk" "$INSTDIR\djview.exe"
  Logset off
SectionEnd
  
Section /o "$(secQuick)" scQuick
  DetailPrint "Creating quick launch shortcut"
  SetShellVarContext all
  LogSet on
  CreateShortCut "$QUICKLAUNCH\DjView.lnk" "$INSTDIR\djview.exe"
  LogSet off
SectionEnd


; --- post install
Section -Post
  WriteUninstaller "$INSTDIR\uninst.exe"
  WriteRegStr HKLM "${PRODUCT_DIR_REGKEY}" "" "$INSTDIR\djview.exe"
  WriteRegStr HKLM "${PRODUCT_DIR_REGKEY}" "Path" "$INSTDIR"
  WriteRegStr ${PRODUCT_UNINST_ROOT_KEY} "${PRODUCT_UNINST_KEY}" "DisplayName" "${PRODUCT_NAME}  ${PRODUCT_VERSION}"
  WriteRegStr ${PRODUCT_UNINST_ROOT_KEY} "${PRODUCT_UNINST_KEY}" "UninstallString" "$INSTDIR\uninst.exe"
  WriteRegStr ${PRODUCT_UNINST_ROOT_KEY} "${PRODUCT_UNINST_KEY}" "DisplayIcon" "$INSTDIR\djview.exe,0"
  WriteRegStr ${PRODUCT_UNINST_ROOT_KEY} "${PRODUCT_UNINST_KEY}" "DisplayVersion" "${PRODUCT_VERSION}"
  WriteRegStr ${PRODUCT_UNINST_ROOT_KEY} "${PRODUCT_UNINST_KEY}" "URLInfoAbout" "${DJVULIBRE_WEB_SITE}"
  WriteRegStr ${PRODUCT_UNINST_ROOT_KEY} "${PRODUCT_UNINST_KEY}" "Publisher" "${PRODUCT_PUBLISHER}"
  ;protect install log
  Rename $INSTDIR\install.log $INSTDIR\uninstall.${PRODUCT_VERSION}
  SetFileAttributes $INSTDIR\uninstall.${PRODUCT_VERSION} HIDDEN|READONLY|SYSTEM
  ${If} $INST_LOG_REN == "1"
    Rename $INSTDIR\install.kik $INSTDIR\install.log
  ${EndIf}
  DetailPrint "-----------------------------------------------------------------------------------------------"
  DetailPrint "Installer NSI V.${NSI_VER} by Kravtsov Konstantin Ivanovich, Novokuznetsk, Russia."
  DetailPrint "-----------------------------------------------------------------------------------------------"
SectionEnd



;------------
; Uninstaller 
;------------


!define UnStrLoc "!insertmacro UnStrLoc"
!macro UnStrLoc ResultVar String SubString StartPoint
  Push "${String}"
  Push "${SubString}"
  Push "${StartPoint}"
  Call un.StrLoc
  Pop "${ResultVar}"
!macroend

Function un.StrLoc
  ; After this point:
  ;  $R0 = StartPoint (input)
  ;  $R1 = SubString (input)
  ;  $R2 = String (input)
  ;  $R3 = SubStringLen (temp)
  ;  $R4 = StrLen (temp)
  ;  $R5 = StartCharPos (temp)
  ;  $R6 = TempStr (temp)*/
  ;Get input from user
  Exch $R0
  Exch
  Exch $R1
  Exch 2
  Exch $R2
  Push $R3
  Push $R4
  Push $R5
  Push $R6
  ;Get "String" and "SubString" length
  StrLen $R3 $R1
  StrLen $R4 $R2
  ;Start "StartCharPos" counter
  StrCpy $R5 0
  ;Loop until "SubString" is found or "String" reaches its end
  ${Do}
    ;Remove everything before and after the searched part ("TempStr")
    StrCpy $R6 $R2 $R3 $R5
    ;Compare "TempStr" with "SubString"
    ${If} $R6 == $R1
      ${If} $R0 == `<`
        IntOp $R6 $R3 + $R5
        IntOp $R0 $R4 - $R6
      ${Else}
        StrCpy $R0 $R5
      ${EndIf}
      ${ExitDo}
    ${EndIf}
    ;If not "SubString", this could be "String"'s end
    ${If} $R5 >= $R4
      StrCpy $R0 ``
      ${ExitDo}
    ${EndIf}
    ;If not, continue the loop
    IntOp $R5 $R5 + 1
  ${Loop}
  ;Return output to user
  Pop $R6
  Pop $R5
  Pop $R4
  Pop $R3
  Pop $R2
  Exch
  Pop $R1
  Exch $R0
FunctionEnd

Function un.RemoveFiles
  Exch $R7 ;get  path+filename uninstall log
  push $0 ;$0 - uninstall log file var
  push $1 ; string readed from file
  push $2 ; string cutted from $1
  push $3 ; int - found str location by StrLoc
  push $4 ; counter
  push $5 ; extracted filename
  DetailPrint "Removing Files.."
  ${If} ${FileExists} "$R7"
    ClearErrors
    FileOpen $0 "$R7" "r"
    ${Do}
      ClearErrors
      FileRead $0 $1
      ${If} ${Errors}
         ;check no file or EOF
         ${ExitDo}
      ${EndIf}
      StrCpy $2 $1 11
      ${If} $2 == "File: wrote"
        ${UnStrloc} $3 $1 " to " ">"
        Intop $3 $3 + 5
        StrCpy $5 $1 -3 $3
        Delete  /REBOOTOK "$5"
      ${ElseIf} $2 == "CreateShort"
        ${UnStrloc} $3 $1 ', in: ' ">"
        Intop $3 $3 - 22
        StrCpy $5 $1 $3 22
        Delete /REBOOTOK "$5"
      ${EndIf}
    ${Loop}
    SetDetailsPrint both
    ;prepare file
    FileSeek $0 0
    ;set counter
    strcpy $4 0
    ${Do}
      ClearErrors
      FileRead $0 $1
      ${If} ${Errors}
         ${ExitDo}
      ${EndIf}
      StrCpy $2 $1 -2 -10
      ${If} $2 == " created"
        ${UnStrloc} $3 $1 " created" ">"
        Intop $3 $3 - 18
        StrCpy $5 $1 $3 18
        push $5
        Intop $4 $4 + 1
      ${EndIf}
    ${Loop}
    ${ForEach} $COUNT $4 1 - 1
      POP $5
      RmDir  /REBOOTOK "$5"
    ${Next}
    FileClose $0
  ${Else}
    RMDir /r "$INSTDIR"  ; too bad
  ${EndIf}
  pop $5 ; extracted filename
  pop $4 ; counter
  pop $3 ; int - found str location by StrLoc
  pop $2 ; string cutted from $1
  pop $1 ; string readed from file
  pop $0 ;$0 - uninstall log file var
  pop $R7 ; path+filename uninstall log
FunctionEnd


Function un.RegisterExt
; Kravtsov Konstantin Ivanovich (C) 2013
; restore assoc from uninst key
; check if restore really need
  Pop $Djvu_EXT
  ClearErrors
  ReadRegStr $TMP_EXT HKLM "${CLASSES}$Djvu_EXT" ""
  ${IfNot} ${Errors}
     ${If} "$TMP_EXT" == ${FILEID}
       ClearErrors
       ReadRegStr $TMP_EXT HKLM "${CLASSES}$Djvu_EXT" ${BACKUP_EXT}
       ${IfNot} ${Errors}
         ${If} "$TMP_EXT" == "${NO_EXT}"
           WriteRegStr HKLM "${CLASSES}$Djvu_EXT" "" ""
         ${Else}
           WriteRegStr HKLM "${CLASSES}$Djvu_EXT" "" $TMP_EXT
         ${EndIf}
         DeleteRegValue HKLM "${CLASSES}$Djvu_EXT" ${BACKUP_EXT}
       ${EndIf}
     ${EndIf}
  ${EndIf}
FunctionEnd

Function un.onUninstSuccess
  IfSilent +2
  MessageBox MB_ICONINFORMATION|MB_OK $(UninstSux_MSG)
FunctionEnd

Function un.onInit
  IfSilent +3
  MessageBox MB_ICONQUESTION|MB_YESNO|MB_DEFBUTTON2 $(Uninst_MSG) IDYES +2
  Abort
FunctionEnd

Section Uninstall
  SetShellVarContext all
  SetFileAttributes $INSTDIR\uninstall.${PRODUCT_VERSION} NORMAL
  IfSilent +2
  SetDetailsPrint both
  ;; remove all files
  push $INSTDIR\uninstall.${PRODUCT_VERSION}
  Call un.RemoveFiles
  Delete "$INSTDIR\uninstall.${PRODUCT_VERSION}"
  Delete "$INSTDIR\uninst.exe"
  RMDir "$INSTDIR"
  ;; clean registry
  DeleteRegKey HKLM "${PRODUCT_DIR_REGKEY}"
  DeleteRegKey HKLM "${CLASSES}${FILEID}"
  DeleteRegKey HKLM "${CLASSES}Applications\djview.exe"
  DeleteRegValue HKLM "${CLASSES}.djvu\OpenWithProgIDs" "${FILEID}"
  DeleteRegValue HKLM "${CLASSES}.djv\OpenWithProgIDs" "${FILEID}"
;if no windjview key  - delete openwithprogid
  ClearErrors
  ReadRegStr $TMP_EXT HKCR "DjVu.Document" ""
  ${If} ${Errors}
  DeleteRegValue HKLM "${CLASSES}.djv\OpenWithProgids" "DjVu.Document"
  DeleteRegValue HKLM "${CLASSES}.djvu\OpenWithProgids" "DjVu.Document"
  ${EndIf}


  Push ".djvu"
  Call un.RegisterExt
  Push ".djv"
  Call un.RegisterExt
  ;; clean uninstall key
  DeleteRegKey ${PRODUCT_UNINST_ROOT_KEY} "${PRODUCT_UNINST_KEY}"
;  SetAutoClose true
  DetailPrint "-----------------------------------------------------------------------------------------------"
  DetailPrint "DjVuLibre installer script V.${NSI_VER} by Kravtsov Konstantin Ivanovich, Novokuznetsk, Russia."
  DetailPrint "-----------------------------------------------------------------------------------------------"
SectionEnd

!include "djvulibrelang-ru.nsh"
!include "djvulibrelang-en.nsh"
!include "djvulibrelang-es.nsh"

; Section descriptions set

!insertmacro MUI_FUNCTION_DESCRIPTION_BEGIN
!insertmacro MUI_DESCRIPTION_TEXT ${scAssoc} $(DESC_Assoc)
!insertmacro MUI_DESCRIPTION_TEXT ${scQuick} $(DESC_Quick)
!insertmacro MUI_DESCRIPTION_TEXT ${scDesk} $(DESC_Desk)
!insertmacro MUI_FUNCTION_DESCRIPTION_END
