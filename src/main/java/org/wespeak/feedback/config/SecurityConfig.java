package org.wespeak.feedback.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Value("${app.security.enabled:false}")
  private boolean securityEnabled;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    if (!securityEnabled) {
      http.csrf(AbstractHttpConfigurer::disable)
          .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
    } else {
      http.csrf(AbstractHttpConfigurer::disable)
          .authorizeHttpRequests(
              auth ->
                  auth.requestMatchers("/actuator/**").permitAll().anyRequest().authenticated());
    }
    return http.build();
  }
}
