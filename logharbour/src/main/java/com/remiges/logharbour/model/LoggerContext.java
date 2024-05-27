package com.remiges.logharbour.model;

import java.util.concurrent.atomic.AtomicBoolean;


public class LoggerContext {
    private LogPriorityLevels minLogPriority;
    private final AtomicBoolean debugMode;

    public LoggerContext(LogPriorityLevels minLogPriority) {
        this.minLogPriority = minLogPriority;
        this.debugMode = new AtomicBoolean(false);
    }

    public synchronized LogPriorityLevels getMinLogPriority() {
        return minLogPriority;
    }

    public synchronized void setMinLogPriority(LogPriorityLevels minLogPriority) {
        this.minLogPriority = minLogPriority;
    }

    public boolean isDebugMode() {
        return debugMode.get();
    }

    public void setDebugMode(boolean debugMode) {
        this.debugMode.set(debugMode);
    }
}
