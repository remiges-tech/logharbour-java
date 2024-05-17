package com.remiges.logharbour.model;

import lombok.Data;

@Data
public class ChangeDetails {
    private String field;
    private Object oldValue;
    private Object newValue;

}
