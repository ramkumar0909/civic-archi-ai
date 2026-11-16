package com.newroniq.civicarch.domain;

import java.util.List;

public record PublishedContent(
    String contentId,
    String title,
    String documentType,
    String locale,
    String updatedAt,
    String payloadHash,
    String status,
    List<Section> sections) {

  public record Section(String heading, String text) {}
}
