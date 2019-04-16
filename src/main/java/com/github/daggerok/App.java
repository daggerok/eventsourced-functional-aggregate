package com.github.daggerok;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.Value;
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

@Value(staticConstructor = "by")
class IncrementCounter {
  private final Integer amount;
}

@Value(staticConstructor = "by")
class CounterIncremented {
  private final Integer amount;
}

@Value(staticConstructor = "by")
class DecrementCounter {
  private final Integer amount;
}

@Value(staticConstructor = "by")
class CounterDecremented {
  private final Integer amount;
}

@ToString
@Component
class CounterAggregate {

  @Getter
  private Long counter = 0L;

  public CounterAggregate handle(Object command) {
    return Match(command).of(
        Case($(instanceOf(IncrementCounter.class)), c -> {
          // validate command
          var amount = c.getAmount();
          Objects.requireNonNull(amount, "amount shouldn't be null");
          if (amount < 1) throw new RuntimeException("amount should be positive");
          // apply event
          return apply(CounterIncremented.by(amount));
        }),
        Case($(instanceOf(DecrementCounter.class)), c -> {
          // validate command
          var amount = c.getAmount();
          Objects.requireNonNull(amount, "amount shouldn't be null");
          if (amount < 1) throw new RuntimeException("amount should be positive");
          // apply event
          return apply(CounterDecremented.by(amount));
        }),
        Case($(), () -> {
          throw new RuntimeException("fuck this command...");
        })
    );
  }

  public CounterAggregate apply(Object event) {
    return Match(event).of(
        Case($(instanceOf(CounterIncremented.class)), e -> {
          // mutate state
          counter += e.getAmount();
          return this;
        }),
        Case($(instanceOf(CounterDecremented.class)), e -> {
          // mutate state
          counter -= e.getAmount();
          return this;
        }),
        Case($(), () -> {
          throw new RuntimeException("fuck this event...");
        })
    );
  }

  public static CounterAggregate recreate(CounterAggregate snapshot, List<Object> events) {
    Objects.requireNonNull(snapshot, "snapshot may not be null.");
    Objects.requireNonNull(events, "events may not be null.");
    return io.vavr.collection.List.ofAll(events)
                                  .foldLeft(snapshot, CounterAggregate::apply);
  }
}

@Slf4j
@SpringBootApplication
@RequiredArgsConstructor
public class App {

  final CounterAggregate aggregate;

  @PostConstruct
  public void init() {
    log.info("empty aggregate 0: {}", aggregate);
    log.info("incremented aggregate by 3: {}", aggregate.handle(IncrementCounter.by(3)));
    log.info("incremented aggregate by 2: {}", aggregate.handle(IncrementCounter.by(2)));
    log.info("decremented aggregate by 1: {}", aggregate.handle(DecrementCounter.by(1)));

    var events = asList(CounterIncremented.by(3),
                        CounterIncremented.by(2),
                        CounterDecremented.by(1));
    var snapshot = new CounterAggregate();
    var recreated = CounterAggregate.recreate(snapshot, events);
    log.info("recreated aggregate: {}", recreated);
  }

  public static void main(String[] args) {
    SpringApplication.run(App.class, args);
  }
}
