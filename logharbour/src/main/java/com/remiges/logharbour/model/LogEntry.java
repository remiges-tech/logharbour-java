package com.remiges.logharbour.model;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode
@Document(indexName = "logger")
public class LogEntry {

    @Id
    private String id;
    private String app;
    private String system;
    private String module;
    private LogType logType;
    private LogPriority pri;
    private String who;
    private String op;
    private String when;
    private String className;
    private String instanceId;
    private Status status;
    private String error;
    private String remoteIP;
    private String msg;
    private Object data; // change data , activity data , Debug data

    public LogEntry(String app, String system, String module, LogPriority pri, String who, String op, Instant now,
            String clazz, String instanceId, Status status, String error, String remoteIP, String message,
            LogData data) {
        this.id = UUID.randomUUID().toString();
        this.app = app;
        this.system = system;
        this.module = module;
        this.pri = pri;
        this.who = who;
        this.when = Instant.now().toString();
        this.op = op;
        this.className = clazz;
        this.instanceId = instanceId;
        this.status = status;
        this.error = error;
        this.remoteIP = remoteIP;
        this.msg = message;
        this.data = data;
    }

    public enum LogPriority {
        DEBUG2,
        DEBUG1,
        DEBUG0,
        INFO,
        WARN,
        ERR,
        CRIT,
        SEC
    }

    public enum LogType {
        CHANGE,
        ACTIVITY,
        DEBUG,
        UNKNOWN;
    }

    public enum Status {
        FAILURE,
        SUCCESS
    }

}
