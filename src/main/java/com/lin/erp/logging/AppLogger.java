package com.lin.erp.logging;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.PrintStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public final class AppLogger {
    private static final Logger LOGGER = Logger.getLogger("com.lin.erp");
    private static boolean initialized;

    private AppLogger() {
    }

    public static synchronized void init() {
        init(true);
    }

    public static synchronized void init(boolean redirectConsole) {
        if (initialized) {
            return;
        }

        try {
            String date = new SimpleDateFormat("yyyyMMdd").format(new Date());
            File logDir = new File(new File("logs"), date);
            if (!logDir.isDirectory() && !logDir.mkdirs()) {
                throw new IOException("Cannot create daily logs directory: " + logDir.getAbsolutePath());
            }

            String runId = createRunId();
            File appLog = new File(logDir, "app-" + runId + ".log");

            FileHandler fileHandler = new FileHandler(appLog.getPath(), false);
            fileHandler.setEncoding(StandardCharsets.UTF_8.name());
            fileHandler.setFormatter(new PlainTextFormatter());
            LOGGER.setUseParentHandlers(false);
            LOGGER.addHandler(fileHandler);
            LOGGER.setLevel(Level.INFO);

            if (redirectConsole) {
                File consoleLog = new File(logDir, "console-" + runId + ".log");
                PrintStream console = new PrintStream(new FileOutputStream(consoleLog, false), true, StandardCharsets.UTF_8.name());
                System.setOut(console);
                System.setErr(console);
            }

            Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread thread, Throwable throwable) {
                    error("Uncaught exception on thread " + thread.getName(), throwable);
                }
            });

            initialized = true;
            info("Logging initialized. Run ID: " + runId + ". App log: " + appLog.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void info(String message) {
        LOGGER.info(message);
    }

    public static void warning(String message) {
        LOGGER.warning(message);
    }

    public static void userAction(String event, String details) {
        StringBuilder builder = new StringBuilder();
        builder.append(">>> USER_ACTION | event=");
        builder.append(sanitize(event));
        if (details != null && details.trim().length() > 0) {
            builder.append(" | ");
            builder.append(sanitize(details));
        }
        LOGGER.info(builder.toString());
    }

    public static void error(String message, Throwable throwable) {
        LOGGER.log(Level.SEVERE, message, throwable);
    }

    private static String sanitize(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\r', ' ').replace('\n', ' ').trim();
    }

    private static String createRunId() {
        String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss-SSS").format(new Date());
        return timestamp + "-" + currentProcessId();
    }

    private static String currentProcessId() {
        String runtimeName = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
        int at = runtimeName.indexOf('@');
        if (at > 0) {
            return runtimeName.substring(0, at);
        }
        return "pid";
    }

    private static final class PlainTextFormatter extends Formatter {
        private final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

        @Override
        public synchronized String format(LogRecord record) {
            StringBuilder builder = new StringBuilder();
            builder.append(format.format(new Date(record.getMillis())));
            builder.append(" ");
            builder.append(record.getLevel().getName());
            builder.append(" ");
            builder.append(record.getLoggerName());
            builder.append(" - ");
            builder.append(formatMessage(record));
            builder.append(System.lineSeparator());
            if (record.getThrown() != null) {
                StringWriter stringWriter = new StringWriter();
                PrintWriter printWriter = new PrintWriter(stringWriter);
                record.getThrown().printStackTrace(printWriter);
                printWriter.flush();
                builder.append(stringWriter);
            }
            return builder.toString();
        }
    }
}
