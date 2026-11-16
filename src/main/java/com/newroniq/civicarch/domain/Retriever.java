package com.newroniq.civicarch.domain;

import java.util.List;

public interface Retriever {

  List<Evidence> retrieve(float[] query, String model, List<String> keys, int limit, double floor);
}
