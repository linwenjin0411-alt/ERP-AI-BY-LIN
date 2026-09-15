using System;
using System.Diagnostics;
using System.Drawing;
using System.IO;
using System.Reflection;
using System.Runtime.InteropServices;
using System.Text;
using System.Threading;
using System.Windows.Forms;

[assembly: AssemblyTitle("Linova One ERP")]
[assembly: AssemblyProduct("Linova One ERP")]
[assembly: AssemblyCompany("Linova")]
[assembly: AssemblyFileVersion("1.0.0.6")]
[assembly: AssemblyInformationalVersion("1.0.0.6")]

internal sealed class StartupForm : Form
{
    public StartupForm()
    {
        Text = "Linova One ERP Startup";
        Width = 460;
        Height = 170;
        FormBorderStyle = FormBorderStyle.FixedDialog;
        MaximizeBox = false;
        MinimizeBox = false;
        StartPosition = FormStartPosition.CenterScreen;
        ShowInTaskbar = true;

        TableLayoutPanel layout = new TableLayoutPanel();
        layout.Dock = DockStyle.Fill;
        layout.Padding = new Padding(22, 18, 22, 18);
        layout.RowCount = 2;
        layout.ColumnCount = 1;
        layout.RowStyles.Add(new RowStyle(SizeType.Percent, 70F));
        layout.RowStyles.Add(new RowStyle(SizeType.Absolute, 28F));

        Label messageLabel = new Label();
        messageLabel.Dock = DockStyle.Fill;
        messageLabel.TextAlign = ContentAlignment.MiddleLeft;
        messageLabel.Font = new Font("Segoe UI", 10F, FontStyle.Regular, GraphicsUnit.Point);
        messageLabel.Text = "Starting Linova One ERP...\r\nアプリケーション JAR を読み込んでいます。しばらくお待ちください。\r\n正在加载应用 JAR，请稍候。\r\nFirst startup after clone may build the JAR and take a few minutes.";
        layout.Controls.Add(messageLabel, 0, 0);

        ProgressBar progress = new ProgressBar();
        progress.Dock = DockStyle.Fill;
        progress.Style = ProgressBarStyle.Marquee;
        progress.MarqueeAnimationSpeed = 28;
        layout.Controls.Add(progress, 0, 1);

        Controls.Add(layout);
    }
}

internal sealed class StartupResult
{
    public int ExitCode = 1;
    public string LogPath = String.Empty;
    public Exception Error;
}

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
                    "run.bat was not found beside the launcher.\r\nランチャーと同じフォルダーに run.bat が見つかりません。\r\n启动器同目录下未找到 run.bat。",
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

            Application.EnableVisualStyles();
            Application.SetCompatibleTextRenderingDefault(false);

            StartupResult result = new StartupResult();
            using (StartupForm startupForm = new StartupForm())
            {
                startupForm.Shown += delegate
                {
                    Thread worker = new Thread(delegate()
                    {
                        try
                        {
                            result.ExitCode = RunBatch(commandProcessor, batchPath, args, rootDir, result);
                        }
                        catch (Exception ex)
                        {
                            result.Error = ex;
                            result.ExitCode = 1;
                        }
                        finally
                        {
                            if (!startupForm.IsDisposed)
                            {
                                startupForm.BeginInvoke(new MethodInvoker(delegate { startupForm.Close(); }));
                            }
                        }
                    });
                    worker.IsBackground = true;
                    worker.Start();
                };
                Application.Run(startupForm);
            }

            if (result.Error != null)
            {
                MessageBox.Show(
                    "Could not start Linova One ERP.\r\nLinova One ERP を起動できませんでした。\r\n无法启动 Linova One ERP。\r\n\r\n" + result.Error.Message,
                    "Linova One ERP",
                    MessageBoxButtons.OK,
                    MessageBoxIcon.Error);
                return 1;
            }

            if (result.ExitCode != 0)
            {
                string logHint = String.IsNullOrEmpty(result.LogPath) ? String.Empty : "\r\n\r\nStartup log / 起動ログ / 启动日志:\r\n" + result.LogPath;
                MessageBox.Show(
                    "Linova One ERP startup failed before the app window opened.\r\nアプリ画面が開く前に起動に失敗しました。\r\n应用窗口打开前启动失败。\r\n\r\nRun run.bat from this folder to see the full error.\r\n詳細なエラーを確認するには、このフォルダーで run.bat を実行してください。\r\n请在当前文件夹运行 run.bat 查看完整错误。\r\n\r\nMake sure JDK and Maven are installed when starting from a fresh clone.\r\nfresh clone から起動する場合は JDK と Maven がインストールされていることを確認してください。\r\n如果是 fresh clone 后首次启动，请确认已安装 JDK 和 Maven。" + logHint,
                    "Linova One ERP",
                    MessageBoxButtons.OK,
                    MessageBoxIcon.Error);
                return result.ExitCode;
            }

            return 0;
        }
        catch (Exception ex)
        {
            MessageBox.Show(
                "Could not start Linova One ERP.\r\nLinova One ERP を起動できませんでした。\r\n无法启动 Linova One ERP。\r\n\r\n" + ex.Message,
                "Linova One ERP",
                MessageBoxButtons.OK,
                MessageBoxIcon.Error);
            return 1;
        }
    }

    private static int RunBatch(string commandProcessor, string batchPath, string[] args, string rootDir, StartupResult result)
    {
        ProcessStartInfo startInfo = new ProcessStartInfo();
        startInfo.FileName = commandProcessor;
        startInfo.Arguments = "/d /s /c " + BuildCmdCommand(batchPath, args);
        startInfo.WorkingDirectory = rootDir;
        startInfo.UseShellExecute = false;
        startInfo.CreateNoWindow = true;
        startInfo.WindowStyle = ProcessWindowStyle.Hidden;
        startInfo.RedirectStandardOutput = true;
        startInfo.RedirectStandardError = true;

        using (StreamWriter logWriter = CreateStartupLog(rootDir, result))
        using (Process process = new Process())
        {
            process.StartInfo = startInfo;
            process.OutputDataReceived += delegate(object sender, DataReceivedEventArgs e)
            {
                WriteLogLine(logWriter, e.Data);
            };
            process.ErrorDataReceived += delegate(object sender, DataReceivedEventArgs e)
            {
                WriteLogLine(logWriter, e.Data);
            };
            WriteLogLine(logWriter, "Starting command: " + startInfo.FileName + " " + startInfo.Arguments);
            process.Start();
            process.BeginOutputReadLine();
            process.BeginErrorReadLine();
            while (!process.WaitForExit(300))
            {
                if (IsLinovaApplicationWindowOpen())
                {
                    WriteLogLine(logWriter, "Application window detected. Closing startup window.");
                    return 0;
                }
            }
            WriteLogLine(logWriter, "Exit code: " + process.ExitCode);
            return process.ExitCode;
        }
    }

    private delegate bool EnumWindowsProc(IntPtr hWnd, IntPtr lParam);

    [DllImport("user32.dll")]
    private static extern bool EnumWindows(EnumWindowsProc enumProc, IntPtr lParam);

    [DllImport("user32.dll")]
    private static extern bool IsWindowVisible(IntPtr hWnd);

    [DllImport("user32.dll", SetLastError = true)]
    private static extern int GetWindowTextLength(IntPtr hWnd);

    [DllImport("user32.dll", SetLastError = true)]
    private static extern int GetWindowText(IntPtr hWnd, StringBuilder text, int count);

    [DllImport("user32.dll")]
    private static extern uint GetWindowThreadProcessId(IntPtr hWnd, out uint processId);

    private static bool IsLinovaApplicationWindowOpen()
    {
        WindowSearchState state = new WindowSearchState(Process.GetCurrentProcess().Id);
        EnumWindows(delegate(IntPtr hWnd, IntPtr lParam)
        {
            if (!IsWindowVisible(hWnd))
            {
                return true;
            }

            uint windowProcessId;
            GetWindowThreadProcessId(hWnd, out windowProcessId);
            if (windowProcessId == state.CurrentProcessId)
            {
                return true;
            }

            string title = GetWindowTitle(hWnd);
            if ("Sign in - Linova One ERP".Equals(title, StringComparison.Ordinal)
                    || "Linova One ERP".Equals(title, StringComparison.Ordinal))
            {
                state.Found = true;
                return false;
            }
            return true;
        }, IntPtr.Zero);
        return state.Found;
    }

    private static string GetWindowTitle(IntPtr hWnd)
    {
        int length = GetWindowTextLength(hWnd);
        if (length <= 0)
        {
            return String.Empty;
        }
        StringBuilder builder = new StringBuilder(length + 1);
        GetWindowText(hWnd, builder, builder.Capacity);
        return builder.ToString();
    }

    private sealed class WindowSearchState
    {
        public readonly uint CurrentProcessId;
        public bool Found;

        public WindowSearchState(int currentProcessId)
        {
            CurrentProcessId = unchecked((uint) currentProcessId);
        }
    }

    private static StreamWriter CreateStartupLog(string rootDir, StartupResult result)
    {
        try
        {
            string logDir = Path.Combine(rootDir, "logs", DateTime.Now.ToString("yyyyMMdd"));
            Directory.CreateDirectory(logDir);
            string logPath = Path.Combine(logDir, "launcher-" + DateTime.Now.ToString("yyyyMMdd-HHmmss-fff") + ".log");
            result.LogPath = logPath;
            return new StreamWriter(logPath, false, System.Text.Encoding.UTF8);
        }
        catch
        {
            result.LogPath = String.Empty;
            return StreamWriter.Null;
        }
    }

    private static void WriteLogLine(StreamWriter writer, string value)
    {
        if (writer == null || value == null)
        {
            return;
        }
        writer.WriteLine(value);
        writer.Flush();
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
