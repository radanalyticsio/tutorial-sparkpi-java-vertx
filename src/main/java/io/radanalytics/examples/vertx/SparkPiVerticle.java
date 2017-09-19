package io.radanalytics.examples.vertx;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.*;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

public class SparkPiVerticle extends AbstractVerticle {

  private static final Logger log = Logger.getRootLogger();

  @Override
  public void start(Future<Void> fut) {

    // Create a router object.
    Router router = Router.router(vertx);

    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        vertx.close();
      }
    });

    String jarFile = config().getString("sparkpi.jarfile");
    log.info("SparkPi submit jar is: "+jarFile);

    // init our Spark context
    if (!SparkContextProvider.init(jarFile)) {
        // masterURL probably not set
        log.error("This application is designed to be run as an oshinko S2I.");
        vertx.close();
        System.exit(1);
    }
    SparkPiProducer pi = new SparkPiProducer();

    router.route("/").handler(routingContext -> {
      HttpServerResponse response = routingContext.response();
      response
          .putHeader("content-type", "text/html")
          .end(pi.GetPi());
    });

    vertx
        .createHttpServer()
        .requestHandler(router::accept)
        .listen(
            config().getInteger("http.port", 8080),
            result -> {
              if (result.succeeded()) {
                fut.complete();
              } else {
                fut.fail(result.cause());
              }
            }
        );
  }
}
