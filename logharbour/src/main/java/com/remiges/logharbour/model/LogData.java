package com.remiges.logharbour.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LogData {
    private ChangeInfo changeData;
    private DebugInfo debugData;
    private String activityData;

    public LogData(ChangeInfo changeData, DebugInfo debugData, String activityData) {
    }

}
