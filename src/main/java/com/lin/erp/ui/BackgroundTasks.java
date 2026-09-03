package com.lin.erp.ui;

import com.lin.erp.logging.AppLogger;

import javax.swing.SwingWorker;
import java.awt.Component;
import java.awt.Cursor;
import java.util.concurrent.ExecutionException;

final class BackgroundTasks {
    private BackgroundTasks() {
    }

    static <T> void run(
            final Component owner,
            final String logMessage,
            final String errorTitle,
            final String errorMessage,
            final Work<T> work,
            final Success<T> success
    ) {
        if (owner != null) {
            owner.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        }
        SwingWorker<T, Void> worker = new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return work.run();
            }

            @Override
            protected void done() {
                try {
                    success.accept(get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    AppLogger.error(logMessage, e);
                    AppMessages.error(owner, errorTitle, errorMessage);
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause() == null ? e : e.getCause();
                    AppLogger.error(logMessage, cause);
                    AppMessages.error(owner, errorTitle, errorMessage);
                } finally {
                    if (owner != null) {
                        owner.setCursor(Cursor.getDefaultCursor());
                    }
                }
            }
        };
        worker.execute();
    }

    interface Work<T> {
        T run() throws Exception;
    }

    interface Success<T> {
        void accept(T value);
    }
}
