package com.newroniq.civicarch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.newroniq.civicarch.domain.CitationPolicy;
import com.newroniq.civicarch.infrastructure.ContentExtractor;
import com.newroniq.civicarch.infrastructure.Sources;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class PolicyTest {

  @Test
  void rejectsFabricatedAndDuplicateReferences() {
    var fabricatedReferences = List.of(3);
    var duplicateReferences = List.of(1, 1);
    assertThatThrownBy(() -> CitationPolicy.validate(fabricatedReferences, 2))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> CitationPolicy.validate(duplicateReferences, 2))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(CitationPolicy.validate(List.of(), 0)).isEmpty();
  }

  @Test
  void rejectsExternalAndEncodedPaths() {
    for (String path :
        List.of(
            "https://evil.test/a", "//evil.test", "/guidance/../secret", "/guidance/%2fsecret")) {
      assertThatThrownBy(() -> Sources.validatePath(path))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Test
  void normalisesHtmlAndExcludesFurniture() throws Exception {
    var mapper = JsonMapper.builder().build();
    var extractor = new ContentExtractor();
    var a =
        extractor.extract(
            mapper.readTree(
                "{\"body\":\"<p>Hello &amp; world</p><h2>Service standard points</h2><p>Ignore</p>\"}"));
    var b = extractor.extract(mapper.readTree("{\"body\":\"<p> Hello   &amp; world </p>\"}"));
    assertThat(a).isEqualTo(b);
    assertThat(extractor.extract(mapper.readTree("null"))).isEmpty();
  }
}
