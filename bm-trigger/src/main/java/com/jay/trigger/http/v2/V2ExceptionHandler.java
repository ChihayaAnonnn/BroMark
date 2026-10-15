package com.jay.trigger.http.v2;

import com.jay.domain.asset.model.*;
import java.util.concurrent.RejectedExecutionException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice(basePackages = "com.jay.trigger.http.v2")
public class V2ExceptionHandler {
  public record ApiError(String code, String info) {}

  private ResponseEntity<ApiError> error(int status, String message) {
    return ResponseEntity.status(status).body(new ApiError(String.valueOf(status), message));
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ApiError> authentication() {
    return error(401, "Unauthorized");
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiError> forbidden() {
    return error(403, "Forbidden");
  }

  @ExceptionHandler({
    IllegalArgumentException.class,
    MethodArgumentNotValidException.class,
    HttpMessageNotReadableException.class
  })
  public ResponseEntity<ApiError> parameter() {
    return error(400, "Invalid request");
  }

  @ExceptionHandler({
    org.springframework.web.bind.ServletRequestBindingException.class,
    org.springframework.beans.TypeMismatchException.class,
    org.springframework.validation.BindException.class,
    jakarta.validation.ConstraintViolationException.class
  })
  public ResponseEntity<ApiError> binding() {
    return error(400, "Invalid request");
  }

  @ExceptionHandler(AssetConflictException.class)
  public ResponseEntity<ApiError> conflict() {
    return error(409, "Request conflicts with an existing operation");
  }

  @ExceptionHandler(BudgetExceededException.class)
  public ResponseEntity<ApiError> limit() {
    return error(429, "Database budget exceeded");
  }

  @ExceptionHandler(RejectedExecutionException.class)
  public ResponseEntity<ApiError> unavailable() {
    return error(503, "Service unavailable");
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> technical(Exception exception) {
    if (exception instanceof org.springframework.web.ErrorResponse response
        && response.getStatusCode().is4xxClientError())
      return error(response.getStatusCode().value(), "Invalid request");
    return error(503, "Service unavailable");
  }
}
