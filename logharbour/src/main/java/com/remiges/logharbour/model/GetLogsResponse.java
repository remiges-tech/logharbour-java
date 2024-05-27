package com.remiges.logharbour.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetLogsResponse {
    private List<LogEntry> logs;
    private int totalLogs;
    private int nrec;
    private String err;
    private String SearchAfterTs;
    private String SearchAfterDocId;
}