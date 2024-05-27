package com.remiges.logharbour.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginUser {

    private String id;
    private String name;
    private String mobile;

}
