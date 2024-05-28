package com.remiges.logharbour.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enum representing different levels of log priorities.
 */
public enum LogPriorityLevels {
    DEBUG2(1, "Debug2"),
    DEBUG1(2, "Debug1"),
    DEBUG0(3, "Debug0"),
    INFO(4, "Info"),
    WARN(5, "Warn"),
    ERR(6, "Err"),
    CRIT(7, "Crit"),
    SEC(8, "Sec"),
    UNKNOWN(0, "Unknown");

    private final int level;
    private final String name;

    /**
     * Constructor for LogPriorityLevels.
     *
     * @param level the numerical level of the log priority
     * @param name  the name of the log priority
     */
    LogPriorityLevels(int level, String name) {
        this.level = level;
        this.name = name;
    }

    @JsonValue
    public String getName() {
        return name;
    }

    /**
     * Creates a LogPriorityLevels instance from the provided name.
     *
     * @param name the name of the log priority level
     * @return the corresponding LogPriorityLevels instance, or UNKNOWN if no match is found
     */
    @JsonCreator
    public static LogPriorityLevels fromName(String name) {
        for (LogPriorityLevels priority : LogPriorityLevels.values()) {
            if (priority.name.equalsIgnoreCase(name)) {
                return priority;
            }
        }
        return UNKNOWN;
    }

    public int getLevel() {
        return level;
    }

    @Override
    public String toString() {
        return name;
    }
}
