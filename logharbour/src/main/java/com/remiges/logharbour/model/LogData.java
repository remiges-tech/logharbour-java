package com.remiges.logharbour.model;

import lombok.Data;

@Data
public class LogData {
    private ChangeInfo changeData;
    private DebugInfo debugData;
    private String activityData;
}
