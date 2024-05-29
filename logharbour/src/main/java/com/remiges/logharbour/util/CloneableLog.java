package com.remiges.logharbour.util;

import com.remiges.logharbour.model.LogEntry.LogPriority;
import com.remiges.logharbour.model.LogEntry.Status;

import lombok.Data;

import com.remiges.logharbour.model.LoggerContext;

@Data
public class CloneableLog implements Cloneable {

    private Params params;

    public CloneableLog(String app, String system, String module, LogPriority pri, String who, String op, String clazz,
            String instanceId, Status status, String error, String remoteIP, LoggerContext loggerContext) {
        this.params = new Params(app, system, module, pri, who, op, clazz, instanceId, status, error, remoteIP,
                loggerContext);
    }

    // Override the clone method
    @Override
    public CloneableLog clone() {
        try {
            CloneableLog cloned = (CloneableLog) super.clone();
            cloned.params = this.params; // Share the same Params object
            return cloned;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }

    public void setApp(String app) {
        this.params.setApp(app);
    }

    @Data
    private static class Params {
        private String app;
        private String system;
        private String module;
        private LogPriority pri;
        private String who;
        private String op;
        private String clazz;
        private String instanceId;
        private Status status;
        private String error;
        private String remoteIP;
        private LoggerContext loggerContext;

        public Params(String app, String system, String module, LogPriority pri, String who, String op, String clazz,
                String instanceId, Status status, String error, String remoteIP, LoggerContext loggerContext) {
            this.app = app;
            this.system = system;
            this.module = module;
            this.pri = pri;
            this.who = who;
            this.op = op;
            this.clazz = clazz;
            this.instanceId = instanceId;
            this.status = status;
            this.error = error;
            this.remoteIP = remoteIP;
            this.loggerContext = loggerContext;
        }

        @Override
        public String toString() {
            return "Params [app=" + app + ", system=" + system + ", module=" + module + ", pri=" + pri + ", who=" + who
                    + ", op=" + op + ", clazz=" + clazz + ", instanceId=" + instanceId + ", status=" + status
                    + ", error=" + error + ", remoteIP=" + remoteIP + ", loggerContext=" + loggerContext + "]";
        }

    }
}
