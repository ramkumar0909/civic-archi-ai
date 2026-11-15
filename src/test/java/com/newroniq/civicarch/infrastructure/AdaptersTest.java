package com.newroniq.civicarch.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.HttpsURLConnection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AdaptersTest {
  @TempDir Path temp;

  @Test
  void validatesRegistryBeforeFetching() throws Exception {
    String valid =
        "sources:\n  - sourceKey: test\n    path: /guidance/test\n    topic: service-standard\n    enabled: true\n";
    Path file = temp.resolve("sources.yml");
    Files.writeString(file, valid);
    assertThat(new Sources(file.toString()).entries()).hasSize(1);
    Files.writeString(file, valid.replace("true", "false"));
    assertThat(new Sources(file.toString()).entries()).isEmpty();
    for (String invalid :
        List.of(
            valid + valid.substring(9),
            valid.replace("test\n    path", "BAD\n    path"),
            valid.replace("service-standard", "unknown"),
            valid.replace("/guidance/test", "//evil.example/a"))) {
      Files.writeString(file, invalid);
      assertThatThrownBy(() -> new Sources(file.toString())).isInstanceOf(Exception.class);
    }
  }

  @Test
  void boundedFetchAndRetryRules() throws Exception {
    var connection = mock(HttpsURLConnection.class);
    var policy = new PolicyProperties();
    var sleeps = new ArrayList<Long>();
    when(connection.getResponseCode()).thenReturn(503, 429, 200);
    when(connection.getInputStream())
        .thenAnswer(_ -> new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8)));
    var client =
        new GovUkClient(
            policy,
            uri -> {
              assertThat(uri.getHost()).isEqualTo("www.gov.uk");
              return connection;
            },
            sleeps::add);
    assertThat(client.fetch("/guidance/test")).isEqualTo("{}".getBytes(StandardCharsets.UTF_8));
    assertThat(sleeps).hasSizeGreaterThanOrEqualTo(2);
    verify(connection, times(3)).setInstanceFollowRedirects(false);
    for (int status : List.of(301, 404, 410, 400, 500)) {
      policy.setAttempts(1);
      when(connection.getResponseCode()).thenReturn(status);
      assertThatThrownBy(() -> client.fetch("/guidance/test"))
          .isInstanceOf(GovUkClient.FetchFailure.class);
    }
    when(connection.getResponseCode()).thenReturn(200);
    when(connection.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[2_000_001]));
    assertThatThrownBy(() -> client.fetch("/guidance/test")).hasMessageContaining("limit");
    when(connection.getResponseCode()).thenThrow(new SocketTimeoutException());
    assertThatThrownBy(() -> client.fetch("/guidance/test"))
        .isInstanceOf(SocketTimeoutException.class);
    assertThatThrownBy(() -> client.fetch("https://evil.example"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void contentSchemasAndSourceStates() throws Exception {
    var client = mock(GovUkClient.class);
    var adapter = new GovUkContentSource(client);
    var source = new com.newroniq.civicarch.domain.SourceRegistry.Source("test", "/guidance/test");
    for (String fixture : List.of("guide", "parts")) {
      byte[] raw;
      try (var input =
          java.util.Objects.requireNonNull(
              getClass().getResourceAsStream("/govuk/" + fixture + ".json"))) {
        raw = input.readAllBytes();
      }
      when(client.fetch(source.path())).thenReturn(raw);
      assertThat(adapter.fetch(source).sections()).hasSize(1);
      assertThat(adapter.fetch(source).status()).isEqualTo("ACTIVE");
    }
    String base = Files.readString(Path.of("src/test/resources/govuk/guide.json"));
    for (String state :
        List.of("withdrawn", "redirect", "empty", "wrongPath", "badId", "malformed")) {
      String raw =
          switch (state) {
            case "withdrawn" -> base.replace("\"details\"", "\"withdrawn\":true,\"details\"");
            case "redirect" -> base.replace("detailed_guide", "redirect");
            case "empty" -> base.replaceAll("<h2>.*</script>", "");
            case "wrongPath" -> base.replace("/guidance/test", "/guidance/other");
            case "badId" -> base.replace("8a079766-b1a7-4701-998e-1eaeae8fb2a5", "bad");
            default -> "{broken";
          };
      when(client.fetch(source.path())).thenReturn(raw.getBytes(StandardCharsets.UTF_8));
      if (List.of("wrongPath", "badId", "malformed").contains(state)) {
        assertThatThrownBy(() -> adapter.fetch(source)).isInstanceOf(Exception.class);
      } else {
        assertThat(adapter.fetch(source).status()).isNotEqualTo("ACTIVE");
      }
    }
    for (int status : List.of(301, 404, 410, 500)) {
      doThrow(new GovUkClient.FetchFailure(status)).when(client).fetch(source.path());
      if (status == 500) {
        assertThatThrownBy(() -> adapter.fetch(source)).isInstanceOf(Exception.class);
      } else {
        assertThat(adapter.fetch(source).sections()).isEmpty();
      }
    }
  }
}
