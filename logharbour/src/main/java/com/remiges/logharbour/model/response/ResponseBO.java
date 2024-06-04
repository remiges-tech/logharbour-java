package com.remiges.logharbour.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResponseBO <T> {
	
	private T data;
	private String status;
	private String statusCode;
	private String message;

}
