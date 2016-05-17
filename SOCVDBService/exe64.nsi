;--------- CONFIGURATION ---------

!define APPNAME "CVFinder"
!define APPDESCRIPTION "CloseVoteFinder"
!define JARFILE "CloseVoteFinder.jar"
!define VERSION 1.0
!define COMPANY "Community"
!define URL http://www.jdd.it

;Uncomment the next line to specify an icon for the EXE.
Icon "CloseVoteFinder.ico"

;Uncomment the next line to specify a splash screen bitmap.
;!define SPLASH_IMAGE "splash.bmp"

;---------------------------------

VIProductVersion 2.0.0.0
VIAddVersionKey ProductName "${APPNAME}"
VIAddVersionKey ProductVersion "${VERSION}"
VIAddVersionKey Comments "Launcher for ${APPDESCRIPTION}"
VIAddVersionKey CompanyName "${COMPANY}"
VIAddVersionKey CompanyWebsite "${URL}"
VIAddVersionKey FileVersion "${VERSION}"
VIAddVersionKey FileDescription "${APPDESCRIPTION}"
VIAddVersionKey LegalCopyright "${COMPANY}"


Name "${APPNAME}.exe"
Caption "${APPNAME}"
OutFile "${APPNAME}.exe"

SilentInstall silent
XPStyle on

!addplugindir .

Section ""
  System::Call "kernel32::CreateMutexA(i 0, i 0, t '${APPNAME}.exe') i .r1 ?e"
  Pop $R0 
  StrCmp $R0 0 +2
  Quit

  ClearErrors
  SetRegView 64
  ReadRegStr $R0 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment" "CurrentVersion"
  ReadRegStr $R0 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment\$R0" "JavaHome"
  IfErrors 0 FoundVM


  ClearErrors
  SetRegView 64
  ReadRegStr $R0 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment" "CurrentVersion"
  ReadRegStr $R0 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment\$R0" "JavaHome"
  IfErrors 0 FoundVM

  ClearErrors
  SetRegView 32
  ReadRegStr $R0 HKLM "SOFTWARE\JavaSoft\Java Development Kit" "CurrentVersion"
  ReadRegStr $R0 HKLM "SOFTWARE\JavaSoft\Java Development Kit\$R0" "JavaHome"
  IfErrors 0 FoundVM

  ClearErrors
  SetRegView 32
  ReadEnvStr $R0 "JAVA_HOME"
  IfErrors NotFound 0

  FoundVM:
  StrCpy $R0 "$R0\bin\javaw.exe"
  IfFileExists $R0 0 NotFound

  StrCpy $R1 ""
  Call GetParameters
  Pop $R1

  SetOverwrite ifdiff
  SetOutPath $EXEDIR
  StrCpy $R0 '$R0 -Xms128m -Dcom.sun.management.jmxremote -Dexe.dir="$EXEDIR" -jar "${JARFILE}" $R1'
  Exec "$R0"

  !ifdef SPLASH_IMAGE
    SetOutPath $TEMP
    File /oname=spltmp.bmp "${SPLASH_IMAGE}"
    Splash::show 2000 "$TEMP\spltmp"
    Delete "$TEMP\spltmp.bmp"
  !endif 

  Sleep 5000
  Quit

  NotFound:
  Sleep 800
  MessageBox MB_ICONEXCLAMATION|MB_YESNO \
                    'Could not find a Java Runtime Environment installed on your computer. \
                     $\nWithout it you cannot run "${APPNAME}". \
                     $\n$\nWould you like to visit the Java website to download it?' \
                    IDNO +2
  ExecShell open "http://java.sun.com/getjava"
  Quit
SectionEnd

Function GetParameters
  Push $R0
  Push $R1
  Push $R2
  StrCpy $R0 $CMDLINE 1
  StrCpy $R1 '"'
  StrCpy $R2 1
  StrCmp $R0 '"' loop
  StrCpy $R1 ' '
  loop:
    StrCpy $R0 $CMDLINE 1 $R2
    StrCmp $R0 $R1 loop2
    StrCmp $R0 "" loop2
    IntOp $R2 $R2 + 1
    Goto loop
  loop2:
    IntOp $R2 $R2 + 1
    StrCpy $R0 $CMDLINE 1 $R2
    StrCmp $R0 " " loop2
  StrCpy $R0 $CMDLINE "" $R2
  Pop $R2
  Pop $R1
  Exch $R0
FunctionEnd