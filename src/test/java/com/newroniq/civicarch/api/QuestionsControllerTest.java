package com.newroniq.civicarch.api;

import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.newroniq.civicarch.application.QuestionService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class QuestionsControllerTest {

  @Test
  void validationAndErrors() throws Exception {
    var service = mock(QuestionService.class);
    var mvc =
        MockMvcBuilders.standaloneSetup(new QuestionsController(service))
            .setControllerAdvice(new Errors())
            .addFilters(
                new RequestLimits(com.newroniq.civicarch.application.RuntimePolicy.defaults()))
            .build();
    for (String body :
        List.of(
            "{\"question\":\"\"}",
            "{\"question\":\"x\",\"maxSources\":-1}",
            "{\"question\":\"x\",\"maxSources\":999}",
            "{broken",
            "{\"question\":\"" + "x".repeat(4001) + "\"}")) {
      mvc.perform(post("/api/questions").contentType(MediaType.APPLICATION_JSON).content(body))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.requestId").exists());
    }
    mvc.perform(
            post("/api/questions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("x".repeat(32769)))
        .andExpect(status().isPayloadTooLarge());
    when(service.answer(anyString(), anyInt(), anyString()))
        .thenThrow(new IllegalStateException("secret"));
    mvc.perform(
            post("/api/questions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"question\":\"hello\"}"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.status").value(503));
  }

  @Test
  void strictJsonContentTypesAndCorrelation() throws Exception {
    var service = mock(QuestionService.class);
    var builder = tools.jackson.databind.json.JsonMapper.builder();
    new JsonConfiguration().strictJson().customize(builder);
    builder.addMixIn(
        org.springframework.http.ProblemDetail.class,
        org.springframework.http.converter.json.ProblemDetailJacksonMixin.class);
    var mvc =
        MockMvcBuilders.standaloneSetup(new QuestionsController(service))
            .setControllerAdvice(new Errors())
            .setMessageConverters(
                new org.springframework.http.converter.json.JacksonJsonHttpMessageConverter(
                    builder.build()))
            .addFilters(
                new RequestLimits(com.newroniq.civicarch.application.RuntimePolicy.defaults()))
            .build();
    for (String body :
        List.of("{\"question\":\"hi\",\"unknown\":true}", "{\"question\":\"hi\"} {}")) {
      var result =
          mvc.perform(post("/api/questions").contentType(MediaType.APPLICATION_JSON).content(body))
              .andExpect(status().isBadRequest())
              .andReturn();
      var parsed = builder.build().readTree(result.getResponse().getContentAsString());
      org.assertj.core.api.Assertions.assertThat(parsed.path("requestId").asText())
          .isEqualTo(result.getResponse().getHeader("X-Request-Id"));
    }
    mvc.perform(post("/api/questions").contentType(MediaType.TEXT_PLAIN).content("hi"))
        .andExpect(status().isUnsupportedMediaType());
    mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                "/api/questions"))
        .andExpect(status().isMethodNotAllowed());
    when(service.answer(anyString(), anyInt(), anyString())).thenThrow(new QuestionService.Busy());
    mvc.perform(
            post("/api/questions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"question\":\"hi\"}"))
        .andExpect(status().isTooManyRequests());
  }

  @Test
  void responseOutcomes() throws Exception {
    var service = mock(QuestionService.class);
    var mvc =
        MockMvcBuilders.standaloneSetup(new QuestionsController(service))
            .addFilters(
                new RequestLimits(com.newroniq.civicarch.application.RuntimePolicy.defaults()))
            .build();
    for (String outcome : List.of("ANSWERED", "INSUFFICIENT_EVIDENCE")) {
      when(service.answer(anyString(), anyInt(), anyString()))
          .thenReturn(
              new QuestionService.Result(
                  "question",
                  "answer",
                  outcome,
                  List.of(),
                  Map.of("returnedChunks", 0),
                  "test",
                  QuestionService.DISCLAIMER));
      mvc.perform(
              post("/api/questions")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"question\":\"hello\"}"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.outcome").value(outcome));
    }
  }
}
