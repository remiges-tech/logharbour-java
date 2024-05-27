package com.remiges.logharbour.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class DebugInfo {
    private int pid;
    private String runtime;
    private String fileName;
    private int lineNumber;
    private String functionName;
    private String stackTrace;
    private Object data;
}
