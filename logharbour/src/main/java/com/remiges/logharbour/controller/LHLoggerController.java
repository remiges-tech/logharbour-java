package com.remiges.logharbour.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.remiges.logharbour.model.LogEntry;
import com.remiges.logharbour.util.LHLogger;

@RestController("/logs")
public class LHLoggerController {

    @Autowired
    private LHLogger logHarbour;

    // get change controller
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
}
