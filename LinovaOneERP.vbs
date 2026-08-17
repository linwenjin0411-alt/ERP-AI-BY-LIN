Option Explicit

Dim shell, fso, rootDir, buildDir, mavenRepo, logsRootDir, logsDir, logFile, appJar
Set shell = CreateObject("WScript.Shell")
Set fso = CreateObject("Scripting.FileSystemObject")

rootDir = fso.GetParentFolderName(WScript.ScriptFullName)
buildDir = fso.BuildPath(rootDir, "build")
mavenRepo = fso.BuildPath(buildDir, "maven-repository")
logsRootDir = fso.BuildPath(rootDir, "logs")
logsDir = fso.BuildPath(logsRootDir, DateStamp())
logFile = fso.BuildPath(logsDir, "startup-" & Timestamp() & ".log")
appJar = fso.BuildPath(rootDir, "target\linova-one-erp.jar")

shell.CurrentDirectory = rootDir
EnsureFolder buildDir
EnsureFolder logsRootDir
EnsureFolder logsDir

If Not CommandExists("mvn") Then
    AppendLog "[ERROR] Apache Maven was not found in PATH."
    Notify "Apache Maven was not found. Install Maven 3.8 or newer, then start Linova One ERP again."
    WScript.Quit 1
End If

Dim buildCommand, buildExitCode
buildCommand = "cmd /c chcp 65001 >nul & mvn -q -DskipTests " & Q("-Dmaven.repo.local=" & mavenRepo) & " package > " & Q(logFile) & " 2>&1"
buildExitCode = shell.Run(buildCommand, 0, True)

If buildExitCode <> 0 Then
    Notify "Linova One ERP failed to build with Maven. See: " & logFile
    WScript.Quit buildExitCode
End If

If Not fso.FileExists(appJar) Then
    AppendLog "[ERROR] Built application jar was not found: " & appJar
    Notify "Linova One ERP build finished, but the application jar was not found. See: " & logFile
    WScript.Quit 1
End If

If HasArgument("--compile-only") Then
    Notify "Compile finished."
    WScript.Quit 0
End If

Dim runCommand
runCommand = "javaw -jar " & Q(appJar)

On Error Resume Next
shell.Run runCommand, 0, False
If Err.Number <> 0 Then
    Notify "Could not start Linova One ERP with javaw. Check your JDK installation."
    WScript.Quit 1
End If
On Error GoTo 0

Sub EnsureFolder(path)
    If Not fso.FolderExists(path) Then
        fso.CreateFolder path
    End If
End Sub

Sub AppendLog(message)
    Dim output
    Set output = fso.OpenTextFile(logFile, 8, True, False)
    output.WriteLine Now & " " & message
    output.Close
End Sub

Function CommandExists(commandName)
    Dim exitCode
    exitCode = shell.Run("cmd /c where " & commandName & " >nul 2>nul", 0, True)
    CommandExists = (exitCode = 0)
End Function

Function Timestamp()
    Timestamp = Year(Now) & Pad2(Month(Now)) & Pad2(Day(Now)) & "-" & _
            Pad2(Hour(Now)) & Pad2(Minute(Now)) & Pad2(Second(Now))
End Function

Function DateStamp()
    DateStamp = Year(Now) & Pad2(Month(Now)) & Pad2(Day(Now))
End Function

Function Pad2(value)
    If Len(CStr(value)) < 2 Then
        Pad2 = "0" & CStr(value)
    Else
        Pad2 = CStr(value)
    End If
End Function

Function Q(value)
    Q = Chr(34) & value & Chr(34)
End Function

Function HasArgument(expected)
    Dim i
    HasArgument = False
    For i = 0 To WScript.Arguments.Count - 1
        If LCase(WScript.Arguments(i)) = LCase(expected) Then
            HasArgument = True
            Exit Function
        End If
    Next
End Function

Sub Notify(message)
    If InStr(1, LCase(WScript.FullName), "cscript.exe", vbTextCompare) > 0 Then
        WScript.Echo message
    Else
        MsgBox message, vbInformation, "Linova One ERP"
    End If
End Sub
