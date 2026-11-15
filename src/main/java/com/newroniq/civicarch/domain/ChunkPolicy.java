package com.newroniq.civicarch.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;

public final class ChunkPolicy {

  private ChunkPolicy() {}

  public static String hash(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  public static List<PublishedContent.Section> chunks(
      List<PublishedContent.Section> sections, int size, int overlap) {
    if (size < 1 || overlap < 0 || overlap >= size) {
      throw new IllegalArgumentException("Invalid chunk bounds");
    }
    var result = new ArrayList<PublishedContent.Section>();
    for (var section : sections) {
      String[] words = section.text().split("\\s+");
      for (int start = 0; start < words.length; start += size - overlap) {
        int end = Math.min(start + size, words.length);
        result.add(
            new PublishedContent.Section(
                section.heading(), String.join(" ", Arrays.copyOfRange(words, start, end))));
        if (end == words.length) {
          break;
        }
      }
    }
    return result;
  }
}
