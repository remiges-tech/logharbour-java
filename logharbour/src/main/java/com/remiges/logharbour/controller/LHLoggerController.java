package com.remiges.logharbour.controller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.remiges.logharbour.model.GetLogsResponse;
import com.remiges.logharbour.model.LogEntry;
import com.remiges.logharbour.model.LogharbourRequestBo;
import com.remiges.logharbour.util.LHLogger;

@RestController
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
        return logHarbour.getLogs(queryToken, app, who, className, instance,op,fromts, tots, ndays,logType,remoteIP,pri,searchAfterTS,searchAfterDocID);
    }




	/**
	 * Retrieves log entries based on the specified parameters.
	 *
	 * @param querytoken Mandatory, the query token of the realm
	 * @param app        Optional, to extract log entries for just the app specified
	 * @param type       Optional, A meaning activity logs, C meaning data-change
	 *                   logs, D meaning debug logs, and omitted meaning all three
	 * @param who        Optional, to extract log entries inserted only by actions
	 *                   performed by the specified user
	 * @param clazz      Optional, to extract logs related to objects of the
	 *                   specified class only
	 * @param instance   Optional, to extract logs only for a specific object
	 *                   instance. This parameter must be null if class is not
	 *                   specified.
	 * @param op         Optional, extract log entries which carry this specific
	 *                   value in their op field
	 * @param fromts     Optional, timestamp, to extract log entries whose when
	 *                   value falls in this time range.
	 * @param tots       Optional, timestamp, to extract log entries whose when
	 *                   value falls in this time range.
	 * @param ndays      Optional, an integer specifying how many days back in time
	 *                   the retrieval must attempt, counting backwards in time from
	 *                   the current date
	 * @param remoteIP   Optional, an IP address in string form specifying the
	 *                   remote IP from where the operation was triggered which
	 *                   generated the log entry
	 * @param pri        Optional, specifies that only logs of priority equal to or
	 *                   higher than the value given here will be returned. If this
	 *                   parameter is present in the call, then data-change log
	 *                   entries are omitted from the result, because those log
	 *                   entries have no priority.
	 * @param setattr    Mandatory, the name of the attribute whose values are
	 *                   requested in the form of a set. The attribute named can
	 *                   only be one of those which have discrete values, i.e. they
	 *                   are conceptually enumerated types. There is no point
	 *                   attempting to perform this operation on an attribute whose
	 *                   values are continuously variable, e.g. when or message. It
	 *                   makes sense only calling this operation for attributes
	 *                   which have a finite number of discrete values, e.g. pri,
	 *                   op, instance, app, type, status, remoteIP, etc.
	 * @return A map containing the set values and an error, if any.
	 * @throws Exception
	 */
	@PostMapping("/getlogs")
	public List<LogEntry>  getSet(@RequestParam(required = true) String queryToken,
			@RequestParam(required = false) String app, @RequestParam(required = false) String type,
			@RequestParam(required = false) String who, @RequestParam(required = false) String clazz,
			@RequestParam(required = false) String instance, @RequestParam(required = false) String op,
			@RequestParam(required = false) LocalDateTime fromts, @RequestParam(required = false) LocalDateTime tots,
			@RequestParam(required = false) Integer ndays, @RequestParam(required = false) String remoteIP,
			@RequestParam(required = false) LogPri_t pri, @RequestParam(required = false) String setattr)
			throws Exception {

		return logHarbour.getSetlogs(convertRequestParamToRequestForm(queryToken, app, type, who, clazz, instance, op, fromts,
				tots, ndays, remoteIP, pri, setattr));
		 
	}

	public static LogharbourRequestBo convertRequestParamToRequestForm(String querytoken, String app, String type,
			String who, String clazz, String instance, String op, LocalDateTime fromts, LocalDateTime tots,
			Integer ndays, String remoteIP, LogPri_t pri, String setattr) {
		return new LogharbourRequestBo(querytoken, app, type, who, clazz, instance, op, fromts, tots, ndays, remoteIP,
				setattr);
	}

	public enum LogPri_t {
		// Define the enum constants based on your use case
		LOW, MEDIUM, HIGH
	}

}
