package demo.altdemo;

import demo.domain.ServiceResult;
import demo.util.ProgArgs;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class TripActorVerticle2 extends AbstractVerticle {
    private static ProgArgs options;

    private static int tripServicePort = 8080;
    private static int theatreServicePort = 8082;
    private static int taxiServicePort = 8084;

    private static String theatreServiceUid = null;
    private static String taxiServiceUid = null;

    private HttpClient httpClient;

    public static void main(String[] args) {
        options = new ProgArgs(args);

        tripServicePort = options.getIntOption("trip.port", tripServicePort);
        theatreServicePort = options.getIntOption("theatre.port", theatreServicePort);
        taxiServicePort = options.getIntOption("taxi.port", taxiServicePort);

        Vertx vertx = Vertx.vertx();

        DeploymentOptions opts = new DeploymentOptions()
                .setInstances(options.getIntOption("parallelism", 10))
                .setConfig(new JsonObject()
                        .put("trip.port", tripServicePort));

        if (options.getBooleanOption("theatre.local", true)) {
            theatreServiceUid = TheatreVerticle.deployService(new String[] {"theatre.port=" + theatreServicePort});
        }

        if (options.getBooleanOption("taxi.local", true)) {
            taxiServiceUid =  TaxiVerticle.deployService(new String[] {"taxi.port=" + taxiServicePort});
        }

        vertx.deployVerticle(TripActorVerticle2.class.getName(), opts);
    }

    private String getServiceName() {
        return "trip";
    }

    @Override
    public void start(Future<Void> future) throws Exception {
        initHttpClient();

        startServer(future, config().getInteger("trip.port"));
    }

    void getRoutes(Router router) {
        router.post(String.format("/api/%s/:name/:taxi", getServiceName())).handler(this::bookTrip);
    }

    private void startServer(Future<Void> future, int listenerPort) {
        Router router = Router.router(vertx);

        getRoutes(router);

        // Create the HTTP server and pass the "accept" method to the request handler.
        vertx
                .createHttpServer()
                .requestHandler(router::accept)
                .listen(listenerPort,
                        result -> {
                            if (result.succeeded()) {
                                future.complete(); // tell the caller the server is ready
                            } else {
                                result.cause().printStackTrace(System.out);
                                future.fail(result.cause()); // tell the caller that server failed to start
                            }
                        }
                );

        assert router.getRoutes().size() > 0;

        String route1 = router.getRoutes().get(0).getPath();

        System.out.printf("%s service listening on http://localhost:%d%s%n" , getServiceName(), listenerPort, route1);
    }

    void initHttpClient() {
        int idleTimeoutSecs = 1;  // TimeUnit.SECONDS
        int connectTimeoutMillis = 1000; // TimeUnit.MILLISECONDS

        httpClient = vertx.createHttpClient(
                new HttpClientOptions().setIdleTimeout(idleTimeoutSecs)
                        .setConnectTimeout(connectTimeoutMillis));;
    }

    private void bookTrip(RoutingContext routingContext) {
        String showName =  routingContext.request().getParam("name");
        String taxiName =  routingContext.request().getParam("taxi");

        Future<String> theatreFuture = Future.future();
        Future<String> taxiFuture = Future.future();

        try {
            bookTheatre(theatreFuture, httpClient, theatreServicePort, showName);
            bookTaxi(taxiFuture, httpClient, taxiServicePort, taxiName);

            CompositeFuture.all(theatreFuture, taxiFuture).setHandler(result -> {
                int status = result.succeeded() ? 201 : 500;
                String msg = result.failed() ? result.cause().getMessage() : "";

                routingContext.response()
                        .setStatusCode(status)
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .end(Json.encodePrettily(new ServiceResult(getServiceName(),
                                Thread.currentThread().getName(), msg, 0, 0)));
            });
        } catch (Exception e) {
            routingContext.response()
                    .setStatusCode(406)
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end(new JsonObject().put("Status", e.getMessage()).encode());
        }
    }

    private void bookTheatre(Future<String> result, HttpClient client, int servicePort, String name) {
        httpClient.post(servicePort, "localhost", "/api/theatre/1")
                .exceptionHandler(e -> result.fail("Theatre booking request failed: " + e.getLocalizedMessage()))
                .handler(h -> result.complete("Theatre booked"))
                .end();
    }

    private void bookTaxi(Future<String> result, HttpClient client, int servicePort, String name) {
        // Book a taxi
        httpClient.post(servicePort, "localhost", "/api/taxi/1")
                .exceptionHandler(e -> result.fail("Taxi booking request failed: " + e.getLocalizedMessage()))
                .handler(h -> result.complete("Taxi booked"))
                .end();
    }
}
