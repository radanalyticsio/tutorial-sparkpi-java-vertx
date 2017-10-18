package io.radanalytics.examples.vertx;

import java.util.LinkedHashMap;
import java.util.Map;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.*;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

public class SparkPiVerticle extends AbstractVerticle {

  private static final Logger log = Logger.getRootLogger();
  private Properties prop = null;

  // vertx-config has more exotic options using Futures
  // and AsyncResult handlers, but this is all we need
  private String loadJarProperty() {
    if (null==prop) {
     prop = new Properties();
    }
    String jarFile = "";
    try {
      InputStream inputStream = getClass().getClassLoader()
                   .getResourceAsStream("sparkpi.properties");
      prop.load(inputStream);
      jarFile = prop.getProperty("sparkpi.jarfile");
    } catch (IOException e) {
        e.printStackTrace();
    }
    return jarFile;
  }

  @Override
  public void start(Future<Void> fut) {
    // Create a router object.
    Router router = Router.router(vertx);
    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        vertx.close();
      }
    });

    String jarFile = this.loadJarProperty();
    log.info("SparkPi submit jar is: "+jarFile);

    // init our Spark context
    if (!SparkContextProvider.init(jarFile)) {
        // masterURL probably not set
        log.error("This application is intended to be run as an oshinko S2I.");
        vertx.close();
        System.exit(1);
    }
    SparkPiProducer pi = new SparkPiProducer();

    router.route("/").handler(routingContext -> {
      HttpServerResponse response = routingContext.response();
      response
          .putHeader("content-type", "text/html")
          .end("Java Vert.x SparkPi server running. Add the 'sparkpi' route to this URL to invoke the app.");
    });

    router.route("/sparkpi").handler(routingContext -> {
      HttpServerResponse response = routingContext.response();
      response
          .putHeader("content-type", "text/html")
          .end(pi.GetPi());
    });

    vertx
        .createHttpServer()
        .requestHandler(router::accept)
        .listen(
            // here developers can make use of vertx-config if they like
            this.config().getInteger("http.port", 8080),
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
