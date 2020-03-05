package com.revature.rms.search;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@EnableEurekaClient
@SpringBootApplication
public class SearchServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(SearchServiceApplication.class, args);
  }
}
