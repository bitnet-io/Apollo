'Start secure transport in background without open console window
'Required for Windows installer
If Not WScript.Arguments.Named.Exists("elevate") Then
  CreateObject("Shell.Application").ShellExecute WScript.FullName _
    , chr(34) & WScript.ScriptFullName  & chr(34) & " " & prgArgs & " " & chr(34) & "/elevate" & chr(34), "", "runas", 1
  WScript.Quit
End If
Set WshShell = CreateObject("WScript.Shell") 
scriptdir = CreateObject("Scripting.FileSystemObject").GetParentFolderName(WScript.ScriptFullName)
WshShell.CurrentDirectory = scriptdir
WshShell.Run chr(34) & scriptdir & "\apl-run-secure-transport.bat" & chr(34), 0, False  
Set fso = CreateObject("Scripting.FileSystemObject")
if fso.FileExists(scriptdir & "\..\..\apollo-desktop\bin\apl-run-desktop.bat") Then 	
	WshShell.CurrentDirectory = scriptdir & "\..\..\apollo-desktop\bin\"
	WshShell.Run chr(34) & "apl-run-desktop.bat" & chr(34) & " secure-transport", 0, False 
End If
