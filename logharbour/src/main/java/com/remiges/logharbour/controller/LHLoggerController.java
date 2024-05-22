package com.remiges.logharbour.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.remiges.logharbour.model.GetLogsResponse;
import com.remiges.logharbour.model.LogEntry;
import com.remiges.logharbour.util.LHLogger;

@RestController
public class LHLoggerController {

    @Autowired
    private LHLogger logHarbour;

    @GetMapping("/data-changes")
    public List<LogEntry> getChanges(
            @RequestParam String queryToken,
            @RequestParam String app,
            @RequestParam(required = false) String who,
            @RequestParam String className,
            @RequestParam String instance,
            @RequestParam(required = false) String field,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) String fromts,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) String tots,
            @RequestParam(required = false, defaultValue = "0") int ndays) throws Exception {
        try {
            return logHarbour.getChanges(queryToken, app, who, className, instance, field, fromts, tots, ndays);
        } catch (IOException e) {
            throw new RuntimeException("Failed to retrieve log entries", e);
        }
    }

    @GetMapping("/data-logs")
    public GetLogsResponse getLogs(
            @RequestParam(required = true) String queryToken,
            @RequestParam(required = false) String app,
            @RequestParam(required = false) String who,
            @RequestParam(required = false) String className,
            @RequestParam(required = false) String instance,
            @RequestParam(required = false) String op,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) String fromts,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) String tots,
            @RequestParam(required = false, defaultValue = "0") int ndays,
            @RequestParam(required = false) String logType,
            @RequestParam(required = false) String remoteIP,
            @RequestParam(required = false) LogEntry.LogPriority pri,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) String searchAfterTS,
            @RequestParam(required = false) String searchAfterDocID) throws Exception {

        // Call the service method to get the changes and return the response
        return logHarbour.getLogs(queryToken, app, who, className, instance, op, fromts, tots, ndays, logType, remoteIP,
                pri);
    }

}
