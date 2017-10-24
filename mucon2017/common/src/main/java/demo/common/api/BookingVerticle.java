package demo.common.api;

import demo.common.domain.ServiceResult;
import demo.common.domain.BookingService;
import io.vertx.core.Future;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public abstract class BookingVerticle extends BookingVerticleImpl {
    private static String SERVICE_NAME = "unset";

    private BookingService serviceClone;

    public BookingVerticle() {
    }

    protected BookingVerticle(String serviceName) {
        SERVICE_NAME = serviceName;
    }

    // concreate classes clone either volatile or persistent STM objects
    protected abstract BookingService getClone();

    @Override
    public void start(Future<Void> future) throws Exception {
        int port = config().getInteger("port", 8080);

        System.out.printf("starting %s on port %d%n", SERVICE_NAME, port);

        serviceClone = getClone();

        startServer(future, port);
    }

    // vertx plumbing and service handlers

    private void startServer(Future<Void> future, int listenerPort) {
        Router router = Router.router(vertx);

        getRoutes(router);

        // Create the HTTP server
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

        System.out.printf("%s: %s service listening on http://%s:%d%s%n",
                Thread.currentThread().getName(), SERVICE_NAME, System.getenv("HOSTNAME"), listenerPort, route1);
    }

    private void getRoutes(Router router) {
        System.out.printf("%s: route: %s%n", SERVICE_NAME, String.format("/api/%s", SERVICE_NAME));
        router.get(String.format("/api/%s", SERVICE_NAME)).handler(this::getBookings);
        router.post(String.format("/api/%s/:seats", SERVICE_NAME)).handler(this::makeBooking);
    }

    private  void getBookings(RoutingContext routingContext) {
        try {
            int bookings = getBookings(serviceClone);
            System.out.printf("%s: reporting %d bookings%n", SERVICE_NAME, bookings);

            routingContext.response()
                    .setStatusCode(201)
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end(Json.encodePrettily(new ServiceResult(
                            SERVICE_NAME, Thread.currentThread().getName(), System.getenv("HOSTNAME"), bookings)));
        } catch (Exception e) {
            System.out.printf("%s: getBookings error: %s clone: %s%n", SERVICE_NAME, e.getMessage(), serviceClone);

            routingContext.response()
                    .setStatusCode(406)
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end(new JsonObject().put("Status", e.getMessage()).encode());
        }
    }

    private void makeBooking(RoutingContext routingContext) {
        try {
            int bookings = makeBooking(serviceClone);

            System.out.printf("%s: made booking (%d)%n", SERVICE_NAME, bookings);

            routingContext.response()
                    .setStatusCode(201)
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end(Json.encodePrettily(new ServiceResult(
                            SERVICE_NAME, Thread.currentThread().getName(), System.getenv("HOSTNAME"), bookings)));
        } catch (Exception e) {
            System.out.printf("%s: makeBooking error: %s clone: %s%n", SERVICE_NAME, e.getMessage(), serviceClone);

            routingContext.response()
                    .setStatusCode(406)
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end(new JsonObject().put("Status", e.getMessage()).encode());
        }
    }
}
