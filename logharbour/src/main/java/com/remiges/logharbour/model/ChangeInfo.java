package com.remiges.logharbour.model;

import java.util.List;

import lombok.Data;

@Data
public class ChangeInfo {

    private String entity;
    private String op;
    private List<ChangeDetails> changes;

}
