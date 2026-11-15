package com.newroniq.civicarch.api;

import com.newroniq.civicarch.application.QuestionService;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
final class Errors {

  @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
  ProblemDetail invalid(Exception e) {
    return problem(400, "Invalid request", "Check question and maxSources.");
  }

  @ExceptionHandler(QuestionService.Busy.class)
  ProblemDetail busy(Exception e) {
    return problem(429, "Capacity exceeded", "Retry later.");
  }

  @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
  ProblemDetail unsupported(Exception e) {
    return problem(415, "Unsupported media type", "Use application/json.");
  }

  @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
  ProblemDetail method(Exception e) {
    return problem(405, "Method not allowed", "Use POST for questions.");
  }

  @ExceptionHandler(Exception.class)
  ProblemDetail unavailable(Exception e) {
    return problem(
        503,
        "Dependency unavailable",
        "Unable to complete request; inspect local dependency health.");
  }

  private ProblemDetail problem(int status, String title, String detail) {
    var p =
        ProblemDetail.forStatusAndDetail(
            org.springframework.http.HttpStatusCode.valueOf(status), detail);
    p.setTitle(title);
    p.setProperty(
        "requestId",
        ((org.springframework.web.context.request.ServletRequestAttributes)
                org.springframework.web.context.request.RequestContextHolder
                    .currentRequestAttributes())
            .getRequest()
            .getAttribute("requestId"));
    return p;
  }
}
