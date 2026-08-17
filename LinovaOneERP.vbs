Option Explicit

Dim shell, fso, rootDir, buildDir, classesDir, sourceRoot, sourcesFile, logsRootDir, logsDir, logFile
Dim libDir, flatlafVersion, flatlafJar
Set shell = CreateObject("WScript.Shell")
Set fso = CreateObject("Scripting.FileSystemObject")

rootDir = fso.GetParentFolderName(WScript.ScriptFullName)
buildDir = fso.BuildPath(rootDir, "build")
classesDir = fso.BuildPath(buildDir, "classes")
sourceRoot = fso.BuildPath(rootDir, "src\main\java")
sourcesFile = fso.BuildPath(buildDir, "sources.txt")
logsRootDir = fso.BuildPath(rootDir, "logs")
logsDir = fso.BuildPath(logsRootDir, DateStamp())
logFile = fso.BuildPath(logsDir, "startup-" & Timestamp() & ".log")
libDir = fso.BuildPath(rootDir, "lib")
flatlafVersion = "3.7.1"
flatlafJar = fso.BuildPath(libDir, "flatlaf-" & flatlafVersion & ".jar")

shell.CurrentDirectory = rootDir
EnsureFolder buildDir
EnsureFolder classesDir
EnsureFolder logsRootDir
EnsureFolder logsDir
WriteSourceList sourceRoot, sourcesFile

Dim compileCommand, compileExitCode
compileCommand = "cmd /c chcp 65001 >nul & javac -encoding UTF-8 -cp " & Q(fso.BuildPath(libDir, "*")) & " -d " & Q(classesDir) & " @" & Q(sourcesFile) & " > " & Q(logFile) & " 2>&1"
compileExitCode = shell.Run(compileCommand, 0, True)

If compileExitCode <> 0 Then
    Notify "Linova One ERP failed to compile. See: " & logFile
    WScript.Quit compileExitCode
End If

If HasArgument("--compile-only") Then
    Notify "Compile finished."
    WScript.Quit 0
End If

EnsureFlatLaf

Dim classPath, runCommand
classPath = classesDir & ";" & fso.BuildPath(rootDir, "lib\*")
runCommand = "javaw -cp " & Q(classPath) & " com.lin.erp.ErpApp"

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

Sub WriteSourceList(folderPath, outputPath)
    Dim output
    Set output = fso.OpenTextFile(outputPath, 2, True, False)
    If fso.FolderExists(folderPath) Then
        AppendJavaFiles fso.GetFolder(folderPath), output
    End If
    output.Close
End Sub

Sub AppendJavaFiles(folder, output)
    Dim file, child
    For Each file In folder.Files
        If LCase(fso.GetExtensionName(file.Name)) = "java" Then
            output.WriteLine Q(Replace(file.Path, "\", "/"))
        End If
    Next
    For Each child In folder.SubFolders
        AppendJavaFiles child, output
    Next
End Sub

Sub EnsureFlatLaf()
    If fso.FileExists(flatlafJar) Then
        Exit Sub
    End If

    EnsureFolder libDir

    Dim url, script, command, exitCode
    url = "https://repo1.maven.org/maven2/com/formdev/flatlaf/" & flatlafVersion & "/flatlaf-" & flatlafVersion & ".jar"
    script = "$ErrorActionPreference='Stop'; [Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -UseBasicParsing -Uri '" & url & "' -OutFile '" & PsSingle(flatlafJar) & "'"
    command = "powershell -NoProfile -ExecutionPolicy Bypass -Command " & Q(script)
    AppendLog "FlatLaf jar missing. Downloading " & flatlafVersion & "."
    exitCode = shell.Run(command, 0, True)

    If exitCode <> 0 Then
        AppendLog "[WARN] FlatLaf download failed. System look and feel will be used."
    Else
        AppendLog "FlatLaf downloaded: " & flatlafJar
    End If
End Sub

Sub AppendLog(message)
    Dim output
    Set output = fso.OpenTextFile(logFile, 8, True, False)
    output.WriteLine Now & " " & message
    output.Close
End Sub

Function PsSingle(value)
    PsSingle = Replace(value, "'", "''")
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
