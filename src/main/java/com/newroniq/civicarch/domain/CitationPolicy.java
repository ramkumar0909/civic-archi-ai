package com.newroniq.civicarch.domain;

import java.util.List;

public final class CitationPolicy {

  private CitationPolicy() {}

  public static List<Integer> validate(List<Integer> selected, int available) {
    if (selected == null
        || selected.size() > available
        || selected.stream().anyMatch(i -> i == null || i < 1 || i > available)
        || selected.stream().distinct().count() != selected.size()) {
      throw new IllegalArgumentException("Invalid evidence selection");
    }
    return List.copyOf(selected);
  }
}
