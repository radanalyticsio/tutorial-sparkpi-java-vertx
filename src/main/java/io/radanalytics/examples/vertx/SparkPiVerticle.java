package io.radanalytics.examples.vertx;

import io.reactivex.Single;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.core.http.HttpServer;
import io.vertx.reactivex.core.http.HttpServerRequest;
import io.vertx.reactivex.core.http.HttpServerResponse;
import io.vertx.reactivex.ext.web.Router;
import org.apache.log4j.Logger;

public class SparkPiVerticle extends AbstractVerticle {

  private static final Logger log = Logger.getRootLogger();
  private JsonObject prop = null;
  private SparkPiProducer pi;

    // vertx-config has more exotic options using Futures
  // and AsyncResult handlers, but this is all we need
  private Single<String> loadJarProperty() {
    if (prop != null) {
     return Single.just(prop.getString("jarfile"));
    }

    return vertx.fileSystem().rxReadFile("sparkpi.json")
            .map(Buffer::toJsonObject)
            .doOnSuccess(json -> prop = json)
            .map(json -> json.getString("jarfile"));
  }

  @Override
  public void start(Future<Void> fut) {
    // Create a router object.
    Router router = Router.router(vertx);
      router.route("/").handler(routingContext -> {
          HttpServerResponse response = routingContext.response();
          response
                  .putHeader("content-type", "text/html")
                  .end("Java Vert.x SparkPi server running. Add the 'sparkpi' route to this URL to invoke the app.");
      });

      router.route("/sparkpi").handler(routingContext -> {
          HttpServerResponse response = routingContext.response();
          HttpServerRequest request = routingContext.request();
          int scale = 2;
          if (request.params().get("scale") != null) {
              scale = Integer.parseInt(request.params().get("scale"));
          }

          int computedScale = scale;
          vertx.<String>rxExecuteBlocking(future -> future.complete(pi.getPi(computedScale)))
              .subscribe(
                  res -> response
                      .putHeader("content-type", "text/html")
                      .end(res),
                  routingContext::fail);

      });

    Single<String> loaded = loadJarProperty()
            .doOnSuccess(jarFile -> {
                log.info("SparkPi submit jar is: " + jarFile);
                if (!SparkContextProvider.init(jarFile)) {
                    throw new RuntimeException("This application is intended to be run as an oshinko S2I.");
                }
                pi = new SparkPiProducer();
            });

      Single<HttpServer> listening = vertx
              .createHttpServer()
              .requestHandler(router::accept)
              .rxListen(this.config().getInteger("http.port", 8080));

      Single.merge(loaded, listening)
              .ignoreElements()
              .subscribe(
                      fut::complete,
                      fut::fail
              );
  }
}
