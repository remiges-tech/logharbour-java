package com.remiges.logharbour.model.response;

import java.util.List;

import com.remiges.logharbour.model.LogEntry;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetLogsResponse {
    private List<LogEntry> logs;
    private Long nrec;
    private String err;
}