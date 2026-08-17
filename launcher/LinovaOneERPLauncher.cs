using System;
using System.Diagnostics;
using System.IO;
using System.Reflection;
using System.Windows.Forms;

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

            string scriptPath = Path.Combine(rootDir, "LinovaOneERP.vbs");
            if (!File.Exists(scriptPath))
            {
                MessageBox.Show(
                    "LinovaOneERP.vbs was not found beside the launcher.",
                    "Linova One ERP",
                    MessageBoxButtons.OK,
                    MessageBoxIcon.Error);
                return 1;
            }

            string wscriptPath = Path.Combine(
                Environment.GetFolderPath(Environment.SpecialFolder.System),
                "wscript.exe");
            if (!File.Exists(wscriptPath))
            {
                wscriptPath = "wscript.exe";
            }

            ProcessStartInfo startInfo = new ProcessStartInfo();
            startInfo.FileName = wscriptPath;
            startInfo.Arguments = "//nologo " + Quote(scriptPath) + BuildArgumentTail(args);
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
        return "\"" + value.Replace("\"", "\\\"") + "\"";
    }
}
