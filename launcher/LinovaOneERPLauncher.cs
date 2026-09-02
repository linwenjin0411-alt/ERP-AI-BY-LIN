using System;
using System.Diagnostics;
using System.IO;
using System.Reflection;
using System.Windows.Forms;

[assembly: AssemblyTitle("Linova One ERP")]
[assembly: AssemblyProduct("Linova One ERP")]
[assembly: AssemblyCompany("Linova")]
[assembly: AssemblyFileVersion("1.0.0.3")]
[assembly: AssemblyInformationalVersion("1.0.0.3")]

public static class LinovaOneERPLauncher
{
    [STAThread]
    public static int Main(string[] args)
    {
        try
        {
            string exePath = Assembly.GetExecutingAssembly().Location;
            string rootDir = Path.GetDirectoryName(exePath);
            if (String.IsNullOrEmpty(rootDir))
            {
                rootDir = Environment.CurrentDirectory;
            }

            string batchPath = Path.Combine(rootDir, "run.bat");
            if (!File.Exists(batchPath))
            {
                MessageBox.Show(
                    "run.bat was not found beside the launcher.",
                    "Linova One ERP",
                    MessageBoxButtons.OK,
                    MessageBoxIcon.Error);
                return 1;
            }

            string commandProcessor = Environment.GetEnvironmentVariable("ComSpec");
            if (String.IsNullOrEmpty(commandProcessor) || !File.Exists(commandProcessor))
            {
                commandProcessor = Path.Combine(
                    Environment.GetFolderPath(Environment.SpecialFolder.System),
                    "cmd.exe");
            }
            if (!File.Exists(commandProcessor))
            {
                commandProcessor = "cmd.exe";
            }

            ProcessStartInfo startInfo = new ProcessStartInfo();
            startInfo.FileName = commandProcessor;
            startInfo.Arguments = "/d /s /c " + BuildCmdCommand(batchPath, args);
            startInfo.WorkingDirectory = rootDir;
            startInfo.UseShellExecute = false;
            startInfo.CreateNoWindow = true;
            startInfo.WindowStyle = ProcessWindowStyle.Hidden;
            Process.Start(startInfo);
            return 0;
        }
        catch (Exception ex)
        {
            MessageBox.Show(
                "Could not start Linova One ERP.\r\n\r\n" + ex.Message,
                "Linova One ERP",
                MessageBoxButtons.OK,
                MessageBoxIcon.Error);
            return 1;
        }
    }

    private static string BuildCmdCommand(string batchPath, string[] args)
    {
        return "\"\"" + EscapeCmdValue(batchPath) + "\"" + BuildArgumentTail(args) + "\"";
    }

    private static string BuildArgumentTail(string[] args)
    {
        if (args == null || args.Length == 0)
        {
            return String.Empty;
        }

        string result = String.Empty;
        for (int i = 0; i < args.Length; i++)
        {
            result += " " + Quote(args[i]);
        }
        return result;
    }

    private static string Quote(string value)
    {
        return "\"" + EscapeCmdValue(value) + "\"";
    }

    private static string EscapeCmdValue(string value)
    {
        return value.Replace("\"", "\"\"");
    }
}
