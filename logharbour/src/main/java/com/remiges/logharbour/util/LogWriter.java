package com.remiges.logharbour.util;

import java.io.PrintWriter;

import com.remiges.logharbour.model.LogPriorityLevels;
import com.remiges.logharbour.model.LoggerContext;

/**
 * LogWriter provides a structured interface for logging messages with various
 * priority levels.
 * It uses a LoggerContext to determine the minimum log priority that should be
 * logged.
 */
public class LogWriter {
    private final LoggerContext context;
    private final PrintWriter writer;
    private LogPriorityLevels priority;

    public LogWriter(LoggerContext context, PrintWriter writer) {
        this.context = context;
        this.writer = writer;
        this.priority = LogPriorityLevels.INFO; // Default priority
    }

    /**
     * Constructs a new LogWriter with the specified LoggerContext, PrintWriter, and
     * log priority.
     * 
     * @param context  the LoggerContext that provides shared state for logging
     * @param writer   the PrintWriter used to write log messages
     * @param priority the log priority level for this LogWriter
     */
    private LogWriter(LoggerContext context, PrintWriter writer, LogPriorityLevels priority) {
        this.context = context;
        this.writer = writer;
        this.priority = priority;
    }

    public LogWriter withPriority(LogPriorityLevels priority) {
        return new LogWriter(context, writer, priority);
    }

    /**
     * Determines whether a log message should be written based on its priority.
     * 
     * @param priority the log priority level of the message
     * @return true if the message should be logged, false otherwise
     */
    public boolean shouldLog(LogPriorityLevels priority) {
        synchronized (context) {
            return priority.ordinal() >= context.getMinLogPriority().ordinal();
        }
    }

    // Logs a message with the specified priority.
    private void log(LogPriorityLevels priority, String msg) {
        if (shouldLog(priority)) {
            writer.println(msg);
            writer.flush();
        }
    }

    public void sec(String msg) {
        log(LogPriorityLevels.SEC, msg);
    }

    public void crit(String msg) {
        log(LogPriorityLevels.CRIT, msg);
    }

    public void err(String msg) {
        log(LogPriorityLevels.ERR, msg);
    }

    public void warn(String msg) {
        log(LogPriorityLevels.WARN, msg);
    }

    public void info(String msg) {
        log(LogPriorityLevels.INFO, msg);
    }

    public void debug0(String msg) {
        log(LogPriorityLevels.DEBUG0, msg);
    }

    public void debug1(String msg) {
        log(LogPriorityLevels.DEBUG1, msg);
    }

    public void debug2(String msg) {
        log(LogPriorityLevels.DEBUG2, msg);
    }
}