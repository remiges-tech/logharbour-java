package com.remiges.logharbour.service;

import java.util.concurrent.atomic.AtomicBoolean;

import com.remiges.logharbour.model.LogEntry;

import lombok.Data;

/**
 * LoggerContext provides a shared context (state) for instances of Logger.
 * It contains a minLogPriority field that determines the minimum log priority
 * level that should be logged by any Logger using this context.
 * The debugMode AtomicBoolean ensures thread-safe operations on the debug mode
 * flag.
 */

@Data
public class LoggerContext {
    private LogEntry.LogPriority minLogPriority;
    private final AtomicBoolean debugMode;

    /**
     * Constructs a new LoggerContext with the specified minimum log priority.
     * 
     * @param minLogPriority the minimum log priority level for this context
     */
    public LoggerContext(LogEntry.LogPriority minLogPriority) {
        this.minLogPriority = minLogPriority;
        this.debugMode = new AtomicBoolean(false);
    }

    public synchronized LogEntry.LogPriority getMinLogPriority() {
        return minLogPriority;
    }

    public synchronized void setMinLogPriority(LogEntry.LogPriority minLogPriority) {
        this.minLogPriority = minLogPriority;
    }

    /**
     * Checks if debug mode is enabled.
     * 
     * @return true if debug mode is enabled, false otherwise
     */
    public boolean isDebugMode() {
        return debugMode.get();
    }

    public void setDebugMode(boolean debugMode) {
        this.debugMode.set(debugMode);
    }
}
