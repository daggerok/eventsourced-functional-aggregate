package com.github.daggerok;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Application main method test")
class AppMainTest {

  @Test
  void main() {
    App.main(new String[0]);
  }
}
