package com.zzzkvidi4.clanmanager.controller;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.shareddata.AsyncMap;

public final class ControllerVerticle extends AbstractVerticle {
  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    MessageConsumer<String> registeredClansConsumer = vertx.eventBus().consumer("clans");
    registeredClansConsumer.handler(messageWrapper -> {
      String newClan = messageWrapper.body();
      System.out.println("Registered new clan: " + newClan);
      MessageConsumer<Long> clanAliveConsumer = vertx.eventBus().consumer(newClan);
      clanAliveConsumer.handler(clanAliveMessageWrapper -> {
        System.out.println("Received clan activity message from " + newClan);
        vertx.sharedData().getLock("clans.active", lockWrapper -> {
          if (lockWrapper.succeeded()) {
            vertx.sharedData().<String, Long>getAsyncMap("clans.active", mapWrapper -> {
              if (mapWrapper.succeeded()) {
                AsyncMap<String, Long> map = mapWrapper.result();
                map.get(newClan, previousUpdateWrapper -> {
                  if (previousUpdateWrapper.succeeded()) {
                    Long previousTimestamp = previousUpdateWrapper.result();
                    Long newTimestamp = clanAliveMessageWrapper.body();
                    if (previousTimestamp == null || previousTimestamp < newTimestamp) {
                      map.put(newClan, newTimestamp, updateWrapper -> lockWrapper.result().release());
                    }
                  }
                });
              }
            });
          }
        });
      });
    });
    startPromise.complete();
  }
}
