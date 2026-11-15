package com.newroniq.civicarch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@org.springframework.boot.context.properties.ConfigurationPropertiesScan
@SpringBootApplication
public class CivicArchApplication {

  public static void main(String[] args) {
    SpringApplication.run(CivicArchApplication.class, args);
  }
}
