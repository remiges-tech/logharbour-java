package com.remiges.logharbour.model;

public class LoggerContext {
    private LogPriorityLevels minLogPriority;

    public LoggerContext(LogPriorityLevels minLogPriority) {
        this.minLogPriority = minLogPriority;
    }

    public synchronized LogPriorityLevels getMinLogPriority() {
        return minLogPriority;
    }

    public synchronized void setMinLogPriority(LogPriorityLevels minLogPriority) {
        this.minLogPriority = minLogPriority;
    }
}
