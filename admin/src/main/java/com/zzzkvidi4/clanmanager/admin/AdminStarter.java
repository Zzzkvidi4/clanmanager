package com.zzzkvidi4.clanmanager.admin;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

public final class AdminStarter {
  public static void main(String[] args) {
    ClusterManager clusterManager = new HazelcastClusterManager();
    VertxOptions vertxOptions = new VertxOptions();
    vertxOptions.setClusterManager(clusterManager);
    if (args.length < 3) {
      throw new RuntimeException("Not enough arguments!");
    }
    Vertx.clusteredVertx(vertxOptions, res -> {
      System.out.println(res.succeeded());
      res.result().deployVerticle(new AdminVerticle(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2])));
    });
  }
}
