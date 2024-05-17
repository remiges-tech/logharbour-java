package com.remiges.logharbour.model;

import lombok.Data;

@Data
public class LogData {
    private ChangeInfo changeDate;
    private DebugInfo debugData;
    private String activeData;
}
