package com.remiges.logharbour.model.request;

import com.remiges.logharbour.model.LogEntry.LogPriority;
import com.remiges.logharbour.model.LogEntry.Status;

import lombok.Data;

@Data
public class LoggerRequest {

    private String id;
    private String mobile;
    private String name;
    private String app;
    private String system;
    private String module;
    private LogPriority logPriority;
    private String who;
    private String op;
    private String clazz;
    private String instanceId;
    private Status status;
    private String additionalInfo;
    private String remoteIP;
    private String message;
    private String error;

}