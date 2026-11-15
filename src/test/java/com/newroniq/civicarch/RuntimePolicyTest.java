package com.newroniq.civicarch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.newroniq.civicarch.application.RuntimePolicy;
import com.newroniq.civicarch.domain.ChunkPolicy;
import com.newroniq.civicarch.domain.CitationPolicy;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class RuntimePolicyTest {
  @Test
  void rejectsUnsafeBounds() {
    double[] defaults = {.65, 10, 20000, 1024, 2, 60000, 280, 40, 32768, 768};
    for (int index = 0; index < defaults.length; index++) {
      for (double value : new double[] {-1, 999999}) {
        var values = defaults.clone();
        values[index] = value;
        assertThatThrownBy(
                () ->
                    new RuntimePolicy(
                        values[0],
                        (int) values[1],
                        (int) values[2],
                        (int) values[3],
                        (int) values[4],
                        (long) values[5],
                        (int) values[6],
                        (int) values[7],
                        (int) values[8],
                        (int) values[9]))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }
    assertThat(RuntimePolicy.defaults().embeddingDimensions()).isEqualTo(768);
    assertThatThrownBy(
            () -> new RuntimePolicy(Double.NaN, 10, 20000, 1024, 2, 60000, 280, 40, 32768, 768))
        .isInstanceOf(IllegalArgumentException.class);
    var emptySections = List.<com.newroniq.civicarch.domain.PublishedContent.Section>of();
    assertThatThrownBy(() -> ChunkPolicy.chunks(emptySections, 1, 1))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> ChunkPolicy.chunks(emptySections, 0, 0))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> ChunkPolicy.chunks(emptySections, 1, -1))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void citationPolicyRejectsAllInvalidReferenceShapes() {
    for (List<Integer> invalid :
        Arrays.asList(
            null,
            List.of(0),
            List.of(-1),
            List.of(2),
            Arrays.asList((Integer) null),
            List.of(1, 1))) {
      assertThatThrownBy(() -> CitationPolicy.validate(invalid, 1))
          .isInstanceOf(IllegalArgumentException.class);
    }
    assertThat(CitationPolicy.validate(List.of(1), 1)).containsExactly(1);
  }
}
