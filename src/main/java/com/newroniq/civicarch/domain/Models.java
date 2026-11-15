package com.newroniq.civicarch.domain;

public interface Models {

  float[] embed(String text);

  String select(String question, String evidence);

  String embeddingIdentity();
}
