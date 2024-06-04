package com.remiges.logharbour.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogData {
    private ChangeInfo changeData;
    private DebugInfo debugData;
    private String activityData;
}
