package com.newroniq.civicarch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.newroniq.civicarch.infrastructure.ContentExtractor;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class ExtractionTest {

  @Test
  void partsSchemaAndEmptyDetails() throws Exception {
    var mapper = JsonMapper.builder().build();
    var extractor = new ContentExtractor();
    assertThat(
            extractor.extract(
                mapper.readTree(
                    "{\"parts\":[{\"title\":\"Part one\",\"body\":\"<h2>Safety</h2><p>Keep systems safe.</p>\"}]}")))
        .containsExactly(
            new com.newroniq.civicarch.domain.PublishedContent.Section(
                "Safety", "Keep systems safe."));
    for (String value : List.of("{}", "{\"body\":\"\"}", "{\"unexpected\":[]}", "null")) {
      assertThat(extractor.extract(mapper.readTree(value))).isEmpty();
    }
    assertThatThrownBy(() -> mapper.readTree("{broken")).isInstanceOf(RuntimeException.class);
  }

  @Test
  void chunksHaveStableOrderAndOverlap() {
    var extractor = new ContentExtractor();
    String text =
        String.join(
            " ", java.util.stream.IntStream.range(0, 600).mapToObj(i -> "word" + i).toList());
    var input =
        List.of(new com.newroniq.civicarch.domain.PublishedContent.Section("Heading", text));
    var chunks = extractor.chunks(input);
    assertThat(chunks).hasSize(3).isEqualTo(extractor.chunks(input));
    assertThat(chunks.get(0).text()).endsWith("word279");
    assertThat(chunks.get(1).text()).startsWith("word240 ");
    assertThat(chunks.get(2).heading()).isEqualTo("Heading");
  }
}
