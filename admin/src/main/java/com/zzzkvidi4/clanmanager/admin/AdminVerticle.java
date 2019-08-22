package com.zzzkvidi4.clanmanager.admin;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Promise;

import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class AdminVerticle extends AbstractVerticle {
  private final String clanName;
  private final int managerCount;
  private final int userCount;
  private long id;

  public AdminVerticle(String clanName, int managerCount, int userCount) {
    this.clanName = clanName;
    this.managerCount = managerCount;
    this.userCount = userCount;
  }

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    vertx.sharedData()
        .getCounter("userCounter", idCounter -> {
          if (idCounter.succeeded()) {
            idCounter.result().incrementAndGet(idWrapper -> {
              if (idWrapper.succeeded()) {
                id = idWrapper.result();
                vertx.sharedData().getLockWithTimeout(clanName + ".admin", 2000L, lock -> {
                  if (!lock.succeeded()) {
                    startPromise.fail("It is not allowed to have multiple admins in one time!");
                    vertx.close();
                    return;
                  }
                  vertx.sharedData().<String, Integer>getAsyncMap(
                      clanName + ".config",
                      mapWrapper -> {
                        ifSucceed(
                            mapWrapper,
                            map -> {
                              map.put(
                                  "managerCount",
                                  managerCount,
                                  managerCountWrapper -> {
                                    ifSucceed(
                                        managerCountWrapper,
                                        val -> {
                                          map.put("userCount", userCount, val2 -> {});
                                        }
                                    );
                                  }
                              );
                            }
                        );
                      }
                  );
                  vertx.eventBus().publish("clans", clanName);
                  vertx.setPeriodic(TimeUnit.SECONDS.toMillis(10), id -> {
                    vertx.eventBus().send(clanName, Instant.now().toEpochMilli());
                  });
                  vertx.setPeriodic(TimeUnit.SECONDS.toMillis(30), id -> {
                    vertx.sharedData().getLock(clanName + ".usersCount", usersLockWrapper -> {
                      if (usersLockWrapper.succeeded()) {
                        vertx.sharedData().getCounter(clanName + ".usersCount", usersCounterWrapper -> {
                          if (usersCounterWrapper.succeeded()) {
                            usersCounterWrapper.result().get(usersCountWrapper -> {
                              if (usersCountWrapper.succeeded()) {
                                Long currentUsersCount = usersCountWrapper.result();
                                System.out.println("Current users count: " + currentUsersCount);
                                System.out.println("Maximal users count: " + userCount);
                                if (currentUsersCount != null && currentUsersCount >= userCount) {
                                  System.out.println("Free space finished, clearing clan.");
                                  vertx.eventBus().publish(clanName + ".clear", true);
                                  usersCounterWrapper.result().compareAndSet(currentUsersCount, 0, res -> {});
                                }
                                usersLockWrapper.result().release();
                              }
                            });
                          }
                        });
                      }
                    });
                  });
                });
              }
            });
          }
        });
  }

  <T> void ifSucceed(AsyncResult<T> async, Consumer<T> action) {
    if (async.succeeded()) {
      action.accept(async.result());
    }
  }
}
