package com.github.daggerok;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@DisplayName("CounterAggregate tests")
class AppTest {

  @Test
  void test_context() {
    log.info("test context...");
    var ctx = new AnnotationConfigApplicationContext(App.class);
    assertThat(ctx).isNotNull();
  }

  @Test
  void test_app() {
    log.info("test App bean context...");
    var ctx = new AnnotationConfigApplicationContext(App.class);
    assertThat(ctx).isNotNull();
    assertThat(ctx.getBean(App.class)).isNotNull();
  }

  @Test
  void test_increment_by_2_command() {
    log.info("test increment command...");
    var ctx = new AnnotationConfigApplicationContext(App.class);
    var aggregate = ctx.getBean(CounterAggregate.class);
    var counter = aggregate.getCounter();
    var incremented = aggregate.handle(IncrementCounter.by(2));
    assertThat(incremented.getCounter()).isEqualTo(counter + 2);
    log.info("incremented: {}", incremented);
  }

  @Test
  void test_CounterAggregate_recreate() {
    log.info("test CounterAggregate.recreate functionality...");
    var ctx = new AnnotationConfigApplicationContext(App.class);
    var snapshot = ctx.getBean(CounterAggregate.class);// 4
    log.info("snapshot aggregate: {}", snapshot);
    assertThat(snapshot.getCounter()).isEqualTo(4);

    var events = asList(CounterIncremented.by(3), // 7 = 4 + 3
                        CounterIncremented.by(2), // 9 = 7 + 2
                        CounterDecremented.by(1));// 8 = 9 - 1
    var base = snapshot.getCounter();
    var recreated = CounterAggregate.recreate(snapshot, events);
    log.info("recreated aggregate: {}", recreated);
    assertThat(recreated.getCounter()).isEqualTo(base + 4);
  }
}
