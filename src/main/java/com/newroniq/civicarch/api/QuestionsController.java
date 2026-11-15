package com.newroniq.civicarch.api;

import com.newroniq.civicarch.application.QuestionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class QuestionsController {

  private final QuestionService service;

  QuestionsController(QuestionService service) {
    this.service = service;
  }

  public record Question(
      @NotBlank @Size(max = 4000) String question, @Min(1) @Max(10) Integer maxSources) {}

  @PostMapping(
      value = "/api/questions",
      consumes = "application/json",
      produces = "application/json")
  public QuestionService.Result ask(
      @Valid @RequestBody Question request, jakarta.servlet.http.HttpServletRequest http) {
    return service.answer(
        request.question(),
        request.maxSources() == null ? 5 : request.maxSources(),
        (String) http.getAttribute("requestId"));
  }
}
