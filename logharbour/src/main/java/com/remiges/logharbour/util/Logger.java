package com.remiges.logharbour.util;

import java.time.Instant;

import com.remiges.logharbour.model.LogData;
import com.remiges.logharbour.model.LogEntry;
import com.remiges.logharbour.model.LogEntry.LogPriority;
import com.remiges.logharbour.model.LogEntry.Status;

public class Logger {
    private String app;
    private String system;
    private String module;
    private LogPriority pri;
    private String who;
    private String op;
    private String clazz; 
    private String instanceId;
    private Status status;
    private String error;
    private String remoteIP;

    public Logger(String app, String system, String module, LogPriority pri, String who, String op,
            String clazz, String instanceId, Status status, String error, String remoteIP) {
        this.app = app;
        this.system = system;
        this.module = module;
        this.pri = pri;
        this.who = who;
        this.op = op;
        this.clazz = clazz;
        this.instanceId = instanceId;
        this.status = status;
        this.error = error;
        this.remoteIP = remoteIP;
    }

    public LogEntry newLogEntry(String message, LogData data) {
        return new LogEntry(app, system, module, pri, who, op, Instant.now(), clazz, instanceId, status, error,
                remoteIP, message, data);
    }
}
