package com.zzzkvidi4.clanmanager.controller;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

public final class ControllerStarter {
  public static void main(String[] args) {
    ClusterManager clusterManager = new HazelcastClusterManager();
    VertxOptions vertxOptions = new VertxOptions();
    vertxOptions.setClusterManager(clusterManager);
    Vertx.clusteredVertx(vertxOptions, vertxWrapper -> {
      if (vertxWrapper.succeeded()) {
        DeploymentOptions deploymentOptions = new DeploymentOptions()
            .setInstances(1);
        vertxWrapper.result().deployVerticle(ControllerVerticle.class, deploymentOptions);
      }
    });
  }
}
