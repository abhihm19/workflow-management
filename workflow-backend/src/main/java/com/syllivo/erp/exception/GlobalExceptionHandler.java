package com.syllivo.erp.exception;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * Ensures API error responses always carry the specific validation message (e.g. insufficient
 * balance, overlapping leave dates) instead of just a generic "Bad Request" body.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<Map<String, Object>> handleResponseStatusException(ResponseStatusException ex) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("timestamp", Instant.now().toString());
		body.put("status", ex.getStatusCode().value());
		body.put("error", ex.getBody().getTitle());
		body.put("message", ex.getReason());
		return ResponseEntity.status(ex.getStatusCode()).body(body);
	}
}
