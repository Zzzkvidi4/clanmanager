package com.zzzkvidi4.clanmanager.manager;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

public final class ManagerStarter {
  public static void main(String[] args) {
    ClusterManager clusterManager = new HazelcastClusterManager();
    VertxOptions vertxOptions = new VertxOptions()
        .setClusterManager(clusterManager);
    if (args.length < 1) {
      throw new RuntimeException("Not enough arguments!");
    }
    Vertx.clusteredVertx(
        vertxOptions,
        vertxWrapper -> {
          if (vertxWrapper.succeeded()) {
            vertxWrapper.result().deployVerticle(new ManagerVerticle(args[0]));
          }
        }
    );
  }
}
