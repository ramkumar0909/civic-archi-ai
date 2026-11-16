package com.newroniq.civicarch.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
final class RequestLimits extends OncePerRequestFilter {

  private final com.newroniq.civicarch.application.RuntimePolicy policy;

  RequestLimits(com.newroniq.civicarch.application.RuntimePolicy policy) {
    this.policy = policy;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String id = UUID.randomUUID().toString();
    response.setHeader("X-Request-Id", id);
    request.setAttribute("requestId", id);
    if (!request.getRequestURI().equals("/api/questions")) {
      chain.doFilter(request, response);
      return;
    }
    byte[] bytes = request.getInputStream().readNBytes(policy.requestBytes() + 1);
    if (bytes.length > policy.requestBytes()) {
      response.setStatus(413);
      response.setContentType("application/problem+json");
      response
          .getWriter()
          .write(
              "{\"type\":\"about:blank\",\"title\":\"Request too large\",\"status\":413,\"detail\":\"Request exceeds configured size limit\",\"requestId\":\""
                  + id
                  + "\"}");
      return;
    }
    chain.doFilter(
        new HttpServletRequestWrapper(request) {

          @Override
          public ServletInputStream getInputStream() {
            var input = new ByteArrayInputStream(bytes);
            return new ServletInputStream() {

              public int read() {
                return input.read();
              }

              public boolean isFinished() {
                return input.available() == 0;
              }

              public boolean isReady() {
                return true;
              }

              public void setReadListener(ReadListener listener) {
                throw new UnsupportedOperationException();
              }
            };
          }
        },
        response);
  }
}
