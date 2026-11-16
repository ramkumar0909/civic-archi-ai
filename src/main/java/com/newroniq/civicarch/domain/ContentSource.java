package com.newroniq.civicarch.domain;

public interface ContentSource {

  PublishedContent fetch(SourceRegistry.Source source)
      throws ContentAcquisitionException, InterruptedException;
}
