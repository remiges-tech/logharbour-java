package com.remiges.logharbour.util;

import java.io.PrintWriter;

import com.remiges.logharbour.model.LogPriorityLevels;
import com.remiges.logharbour.model.LoggerContext;

public class LogWriter {
    private final LoggerContext context;
    private final PrintWriter writer;
    private LogPriorityLevels priority;

    public LogWriter(LoggerContext context, PrintWriter writer) {
        this.context = context;
        this.writer = writer;
        this.priority = LogPriorityLevels.INFO; // Default priority
    }

    private LogWriter(LoggerContext context, PrintWriter writer, LogPriorityLevels priority) {
        this.context = context;
        this.writer = writer;
        this.priority = priority;
    }

    public LogWriter withPriority(LogPriorityLevels priority) {
        return new LogWriter(context, writer, priority);
    }

    public boolean shouldLog(LogPriorityLevels priority) {
        synchronized (context) {
            return priority.ordinal() >= context.getMinLogPriority().ordinal();
        }
    }

    private void log(LogPriorityLevels priority, String msg) {
        if (shouldLog(priority)) {
            writer.println(msg);
            writer.flush();
        }
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