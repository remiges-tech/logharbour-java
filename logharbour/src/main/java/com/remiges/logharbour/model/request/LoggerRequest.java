package com.remiges.logharbour.model.request;

import com.remiges.logharbour.model.LogEntry.LogPriority;
import com.remiges.logharbour.model.LogEntry.Status;

import lombok.Data;

@Data
public class LoggerRequest {

    private String userId;
    private String userName;
    private String password;
    private String logSource;
    private String logCategory;
    private String module;
    private LogPriority logPriority;
    private String user;
    private String action;
    private String loggerClassName;
    private String instanceId;
    private Status status;
    private String additionalInfo;
    private String ipAddress;
    private String message;
}
