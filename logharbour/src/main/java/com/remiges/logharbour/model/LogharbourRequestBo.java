package com.remiges.logharbour.model;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LogharbourRequestBo {
    private String queryToken;
    private String app;
    private String type;
    private String who;
    private String className;
    private String instance;
    private String op;
    private LocalDateTime fromTs;
    private LocalDateTime toTs;
    private Integer nDays;
    private String remoteIP;
    private String setAttr;

}
