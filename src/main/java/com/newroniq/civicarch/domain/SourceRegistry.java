package com.newroniq.civicarch.domain;

import java.util.List;

public interface SourceRegistry {

  record Source(String sourceKey, String path) {}

  List<Source> entries();
}
