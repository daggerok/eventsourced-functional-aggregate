package com.github.daggerok;

import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Objects;

import static io.vavr.API.*;
import static io.vavr.Predicates.instanceOf;
import static java.util.Arrays.asList;

class DoSomethingCommand {}

class SomethingIsDoneEvent {}

class DoSomethingMoreCommand {}

class SomethingMoreAlsoDoneEvent {}

@ToString
@Component
class MyAggregate {

  private Long counter = 0L;

  public MyAggregate apply(Object event) {
    return Match(event).of(
        Case($(instanceOf(SomethingIsDoneEvent.class)), e -> {
          counter++;
          return this;
        }),
        Case($(instanceOf(SomethingMoreAlsoDoneEvent.class)), e -> {
          counter--;
          return this;
        }),
        Case($(), () -> {
          throw new RuntimeException("fuck it...");
        })
    );
  }

  public static MyAggregate recreate(MyAggregate snapshot, List<Object> events) {
    Objects.requireNonNull(snapshot, "snapshot may not be null.");
    Objects.requireNonNull(events, "events may not be null.");
    return io.vavr.collection.List.ofAll(events)
                                  .foldLeft(snapshot, MyAggregate::apply);
  }
}

@Slf4j
@SpringBootApplication
@RequiredArgsConstructor
public class App {

  final MyAggregate aggregate;

  @PostConstruct
  public void init() {
    log.info("aggregate 0: {}", aggregate);
    log.info("aggregate 1: {}", aggregate.apply(new SomethingIsDoneEvent()));
    log.info("aggregate 2: {}", aggregate.apply(new SomethingIsDoneEvent()));
    log.info("aggregate 3: {}", aggregate.apply(new SomethingMoreAlsoDoneEvent()));

    List<Object> events = asList(new SomethingIsDoneEvent(),
                                 new SomethingIsDoneEvent(),
                                 new SomethingMoreAlsoDoneEvent());
    MyAggregate snapshot = new MyAggregate();
    MyAggregate recreated = MyAggregate.recreate(snapshot, events);
    log.info("aggregate 4: {}", recreated);
  }

  public static void main(String[] args) {
    SpringApplication.run(App.class, args);
  }
}
