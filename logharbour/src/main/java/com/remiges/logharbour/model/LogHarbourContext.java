package com.remiges.logharbour.model;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * LoggerContext provides a shared context (state) for instances of Logger.
 * It contains a minLogPriority field that determines the minimum log priority
 * level that should be logged by any Logger using this context.
 * The debugMode AtomicBoolean ensures thread-safe operations on the debug mode
 * flag.
 */
public class LogHarbourContext {
    private LogPriorityLevels minLogPriority;
    private final AtomicBoolean debugMode;

    /**
     * Constructs a new LoggerContext with the specified minimum log priority.
     * 
     * @param minLogPriority the minimum log priority level for this context
     */
    public LogHarbourContext(LogPriorityLevels minLogPriority) {
        this.minLogPriority = minLogPriority;
        this.debugMode = new AtomicBoolean(false);
    }

    public synchronized LogPriorityLevels getMinLogPriority() {
        return minLogPriority;
    }

    public synchronized void setMinLogPriority(LogPriorityLevels minLogPriority) {
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
