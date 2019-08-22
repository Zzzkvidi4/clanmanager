package com.zzzkvidi4.clanmanager.user;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

public final class UserStarter {
  public static void main(String[] args) {
    ClusterManager clusterManager = new HazelcastClusterManager();
    VertxOptions vertxOptions = new VertxOptions()
        .setClusterManager(clusterManager);
    Vertx.clusteredVertx(
        vertxOptions,
        vertxWrapper -> {
          if (vertxWrapper.succeeded()) {
            vertxWrapper.result().deployVerticle(new UserVerticle());
          }
        }
    );
  }
}
