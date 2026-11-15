package com.newroniq.civicarch.infrastructure;

import com.newroniq.civicarch.domain.PublishedContent.Section;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import org.jsoup.Jsoup;
import tools.jackson.databind.JsonNode;

public final class ContentExtractor {

  public static String hash(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (java.security.NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  public List<Section> extract(JsonNode details) {
    List<Section> result = new ArrayList<>();
    if (details == null || details.isNull()) {
      return result;
    }
    if (details.path("body").isTextual()) {
      html(details.path("body").asText(), result);
    }
    for (JsonNode section : details.path("parts")) {
      if (section.path("body").isTextual()) {
        result.add(new Section(section.path("title").asText(""), ""));
        html(section.path("body").asText(), result);
      }
    }
    return result.stream().filter(s -> !s.text().isBlank()).toList();
  }

  private void html(String html, List<Section> result) {
    var doc = Jsoup.parseBodyFragment(html);
    doc.select("script,style,nav,footer").remove();
    String heading = "Introduction";
    StringBuilder text = new StringBuilder();
    for (var el : doc.select("h1,h2,h3,h4,p,li")) {
      if (el.tagName().matches("h[1-4]")) {
        if (!text.isEmpty()) {
          result.add(new Section(heading, text.toString().strip()));
        }
        text.setLength(0);
        heading = el.text();
        if (Set.of("Related guidance", "Service standard points", "Contact us").contains(heading)) {
          break;
        }
      } else if (el.parents().stream().noneMatch(p -> p.tagName().equals("li"))) {
        appendText(el.text(), text);
      }
    }
    if (!text.isEmpty()) {
      result.add(new Section(heading, text.toString().strip()));
    }
  }

  private void appendText(String value, StringBuilder text) {
    String line = value.replaceAll("[\\s\\u00a0\\u202f]+", " ").strip();
    if (!line.isBlank()) {
      text.append(line).append('\n');
    }
  }

  public List<Section> chunks(List<Section> sections) {
    var result = new ArrayList<Section>();
    for (var section : sections) {
      String[] words = section.text().split("\\s+");
      for (int start = 0; start < words.length; start += 240) {
        int end = Math.min(start + 280, words.length);
        result.add(
            new Section(
                section.heading(), String.join(" ", Arrays.copyOfRange(words, start, end))));
        if (end == words.length) {
          break;
        }
      }
    }
    return result;
  }
}
