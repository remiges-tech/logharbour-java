package com.remiges.logharbour.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChangeDetails {
    private String field;
    private Object oldValue;
    private Object newValue;

}
