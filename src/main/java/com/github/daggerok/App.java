package com.github.daggerok;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

@SuppressWarnings("unchecked")
abstract class AbstractAggregate<A extends AbstractAggregate> {

  public abstract void configure();

  protected Map<Class<?>, Function<?, ?>> commands = new ConcurrentHashMap<>();
  protected Map<Class<?>, Function<?, A>> events = new ConcurrentHashMap<>();

  protected <C, E, T extends Class<C>> A withCommand(T type, Function<C, E> handler) {
    commands.put(type, handler);
    return (A) this;
  }

  protected <E, T extends Class<E>> A withEvent(T type, Function<E, A> mapper) {
    events.put(type, mapper);
    return (A) this;
  }

  public <C, E> A handle(C command) {
    var type = Objects.requireNonNull(command, "command may not be null")
                      .getClass();
    if (!commands.containsKey(type)) {
      String errorMessage = format("command %s is not supported", type);
      throw new IllegalStateException(errorMessage);
    }
    var commandHandler = (Function<C, E>) commands.get(type);
    var event = commandHandler.apply(command);
    return apply(event);
  }

  public <E> A apply(E event) {
    var type = Objects.requireNonNull(event, "event may not be null")
                      .getClass();
    if (!events.containsKey(type)) {
      String errorMessage = format("event handler %s cannot be found", type);
      throw new IllegalStateException(errorMessage);
    }
    var eventMapper = (Function<E, A>) events.get(type);
    return eventMapper.apply(event);
  }

  public <E> A applyAll(A snapshot, List<E> events) {
    Objects.requireNonNull(snapshot, "snapshot may not be null");
    Objects.requireNonNull(events, "events may not be null");
    var result = snapshot;
    for (Object event : events) {
      result = (A) result.apply(event);
    }
    return result;
  }

  public static <A extends AbstractAggregate<A>> A recreate(A snapshot, List<Object> events) {
    Objects.requireNonNull(snapshot, "snapshot may not be null");
    Objects.requireNonNull(events, "events may not be null");
    return io.vavr.collection.List.ofAll(events)
                                  .foldLeft(snapshot, AbstractAggregate::apply); // requires configure() execution...
  }
}

@Value(staticConstructor = "by")
class IncrementCounter {
  private final Integer value;
}

@Value(staticConstructor = "by")
class CounterIncremented {
  private final Integer value;
}

@Value(staticConstructor = "by")
class DecrementCounter {
  private final Integer value;
}

@Value(staticConstructor = "by")
class CounterDecremented {
  private final Integer value;
}

@ToString
@Component
@EqualsAndHashCode(callSuper = false)
class CounterAggregate extends AbstractAggregate<CounterAggregate> {

  @Getter
  private Long counter = 0L;

  @Override
  @PostConstruct
  public void configure() {
    this
        .withCommand(IncrementCounter.class, c -> {
          if (c.getValue() < 1)
            throw new IllegalStateException("increment command value should be positive");
          return CounterIncremented.by(c.getValue()); // here eventStore can save event and produce it as return result
        })
        .withEvent(CounterIncremented.class, e -> {
          this.counter += e.getValue();
          return this;
        })
        .withCommand(DecrementCounter.class, c -> {
          if (c.getValue() < 1)
            throw new IllegalStateException("decrement command value should be positive");
          return CounterDecremented.by(c.getValue());
        })
        .withEvent(CounterDecremented.class, e -> {
          this.counter -= e.getValue();
          return this;
        });
  }
}

@Slf4j
@SpringBootApplication
public class App {

  public static void main(String[] args) {
    var context = SpringApplication.run(App.class, args);
    var aggregate = context.getBean(CounterAggregate.class);

    log.info("empty aggregate 0: {}", aggregate);
    log.info("incremented aggregate by 3: {}", aggregate.handle(IncrementCounter.by(3)));
    log.info("incremented aggregate by 2: {}", aggregate.handle(IncrementCounter.by(2)));
    log.info("decremented aggregate by 1: {}", aggregate.handle(DecrementCounter.by(1)));

    var snapshot = new CounterAggregate(); // from beginning...
    snapshot.configure();

    var events = asList(CounterIncremented.by(3),
                        CounterIncremented.by(2),
                        CounterDecremented.by(1));
    var recreated = snapshot.applyAll(snapshot, events);
    log.info("recreated aggregate: {}", recreated);

    log.info("aggregates are same: {}", aggregate.equals(recreated));

    var snapshot2 = new CounterAggregate();
    snapshot2.configure();

    var recreated2 = CounterAggregate.recreate(snapshot2, events);
    log.info("recreated 2nd aggregate: {}", recreated2);
  }
}
