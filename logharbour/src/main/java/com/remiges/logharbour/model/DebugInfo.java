package com.remiges.logharbour.model;

import lombok.Data;

@Data
public class DebugInfo {
    private int pid;
    private String runtime;
    private String fileName;
    private int lineNumber;
    private String functionName;
    private String stackTrace;
    private Object data;
}
