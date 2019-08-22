package com.zzzkvidi4.clanmanager.user;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.shareddata.AsyncMap;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

public final class UserVerticle extends AbstractVerticle {
  private static final Random random = new Random(System.currentTimeMillis());
  private long id;
  private boolean shouldSendOrder = true;

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    vertx.sharedData().getCounter("userCounter", idCounterWrapper -> {
      if (idCounterWrapper.succeeded()) {
        idCounterWrapper.result().incrementAndGet(idWrapper -> {
          if (idWrapper.succeeded()) {
            id = idWrapper.result();
            startUserBehaviour(startPromise);
          }
        });
      }
    });
  }

  private void startUserBehaviour(Promise<Void> startPromise) {
    vertx.sharedData().<String, Long>getAsyncMap("clans.active", mapWrapper -> {
      if (mapWrapper.succeeded()) {
        long currentTime = Instant.now().toEpochMilli();
        mapWrapper.result().entries(clansWrapper -> {
          if (clansWrapper.succeeded()) {
            Map<String, Long> activeClans = clansWrapper.result();
            List<String> activeClansNames = activeClans.entrySet()
                .stream()
                .filter(e -> e.getValue().longValue() >= currentTime - TimeUnit.SECONDS.toMillis(30))
                .map(Map.Entry::getKey)
                .collect(toList());
            if (activeClansNames.isEmpty()) {
              if (startPromise != null) {
                startPromise.fail("No active clans!");
              }
              vertx.close();
            }
            String activeClan = activeClansNames.get(random.nextInt(activeClansNames.size()));
            System.out.println("Selected clan: " + activeClan);
            vertx.setPeriodic(TimeUnit.SECONDS.toMillis(10), periodicId -> {
              if (shouldSendOrder) {
                System.out.println("Sending order.");
                vertx.eventBus().<Boolean>request(activeClan + ".order", 1, replyWrapper -> {
                  if (replyWrapper.succeeded()) {
                    shouldSendOrder = !replyWrapper.result().body();
                    System.out.println("Receiver response from manager: " + !shouldSendOrder);
                    if (!shouldSendOrder) {
                      MessageConsumer<String> consumer = vertx.eventBus().consumer(activeClan + ".chat");
                      consumer.handler(messageWrapper -> System.out.println("Received message: " + messageWrapper.body()));
                      vertx.eventBus()
                          .consumer(activeClan + ".clear")
                          .handler(obj -> {
                            System.out.println("Received clearing message.");
                            shouldSendOrder = true;
                            startUserBehaviour(null);
                          });
                    }
                  }
                });
              } else {
                System.out.println("Sending message.");
                vertx.eventBus().send(activeClan + ".chat", "some message");
              }
            });
          }
        });
      }
    });
  }
}
