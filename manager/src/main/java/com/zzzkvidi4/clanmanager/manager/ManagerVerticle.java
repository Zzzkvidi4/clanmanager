package com.zzzkvidi4.clanmanager.manager;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.shareddata.AsyncMap;
import io.vertx.core.shareddata.Counter;

public final class ManagerVerticle extends AbstractVerticle {
  private final String clanName;
  private long id;

  public ManagerVerticle(String clanName) {
    this.clanName = clanName;
  }

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    vertx.sharedData().getCounter("userCounter", idCounterWrapper -> {
      if (idCounterWrapper.succeeded()) {
        idCounterWrapper.result().incrementAndGet(idWrapper -> {
          if (idWrapper.succeeded()) {
            this.id = idWrapper.result();
            vertx.sharedData().getLock(clanName + ".managers", lockWrapper -> {
              if (lockWrapper.succeeded()) {
                vertx.sharedData().<String, Integer>getAsyncMap(clanName + ".config", mapWrapper -> {
                  if (mapWrapper.succeeded()) {
                    AsyncMap<String, Integer> clanConfigMap = mapWrapper.result();
                    clanConfigMap.get("managerCount", maximalManagerCountWrapper -> {
                      if (maximalManagerCountWrapper.succeeded()) {
                        Integer managerCount = maximalManagerCountWrapper.result();
                        if (managerCount == null) {
                          startPromise.fail("Clan not started!");
                          lockWrapper.result().release();
                          vertx.close();
                        }
                        vertx.sharedData().getCounter(clanName + ".managerCount", managerCounterWrapper -> {
                          if (managerCounterWrapper.succeeded()) {
                            Counter managerCounter = managerCounterWrapper.result();
                            managerCounter.getAndIncrement(managerCountWrapper -> {
                              if (managerCountWrapper.succeeded()) {
                                Long currentManagerCount = managerCountWrapper.result();
                                if (currentManagerCount >= managerCount) {
                                  startPromise.fail("Too many managers for this clan!");
                                  lockWrapper.result().release();
                                  vertx.close();
                                }
                                MessageConsumer<String> orderConsumer = vertx.eventBus().consumer(clanName + ".order");
                                orderConsumer.handler(orderMessageWrapper -> {
                                  System.out.println("Received order");
                                  vertx.sharedData().getLock(clanName + ".users", usersLockWrapper -> {
                                    if (usersLockWrapper.succeeded()) {
                                      clanConfigMap.get("userCount", maximalUserCountWrapper -> {
                                        if (maximalUserCountWrapper.succeeded()) {
                                          Integer maximalUserCount = maximalUserCountWrapper.result();
                                          if (maximalUserCount == null) {
                                            startPromise.fail("Clan not started!");
                                            usersLockWrapper.result().release();
                                            vertx.close();
                                          }
                                          vertx.sharedData().getCounter(clanName + ".usersCount", usersCounterWrapper -> {
                                            if (usersCounterWrapper.succeeded()) {
                                              Counter usersCounter = usersCounterWrapper.result();
                                              usersCounter.getAndIncrement(usersCountWrapper -> {
                                                if (usersCounterWrapper.succeeded()) {
                                                  long userCount = usersCountWrapper.result();
                                                  orderMessageWrapper.reply(userCount < maximalUserCount);
                                                }
                                                usersLockWrapper.result().release();
                                              });
                                            }
                                          });
                                        }
                                      });
                                    }
                                  });
                                });
                                lockWrapper.result().release();
                              }
                            });
                          }
                        });
                      }
                    });
                  }
                });
              }
            });
          }
        });
      }
    });
  }
}
