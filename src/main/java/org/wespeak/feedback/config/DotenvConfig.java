package org.wespeak.feedback.config;

import io.github.cdimascio.dotenv.Dotenv;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class DotenvConfig {

  @PostConstruct
  public void loadEnv() {
    try {
      Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

      dotenv
          .entries()
          .forEach(
              entry -> {
                System.setProperty(entry.getKey(), entry.getValue());
              });

      log.info("Loaded .env file into system properties");
    } catch (Exception e) {
      log.warn("Could not load .env file: {}", e.getMessage());
    }
  }
}
