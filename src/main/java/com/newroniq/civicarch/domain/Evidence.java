package com.newroniq.civicarch.domain;

import java.time.Instant;
import java.util.UUID;

public record Evidence(
    UUID chunkId,
    String sourceKey,
    String title,
    String url,
    String contentId,
    Instant publishedUpdatedAt,
    Instant retrievedAt,
    String section,
    String excerpt,
    String freshness,
    double similarity) {}
