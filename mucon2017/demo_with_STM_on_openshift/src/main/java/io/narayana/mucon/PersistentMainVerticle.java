package io.narayana.mucon;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import org.jboss.stm.Container;

public class PersistentMainVerticle extends AbstractVerticle {
    private static Container<FlightService> container;
    private static FlightService flightService;

    public static void main(String[] args) {
        System.out.printf("DEPLOYING PersistentMainVerticle%n");
        flightService = Helper.getFlightService();

        Vertx.vertx().deployVerticle(PersistentMainVerticle.class.getName(), new DeploymentOptions().setInstances(8));
    }

    public void start() {
        FlightService flightService = Helper.getFlightService();
        Router router = Router.router(vertx);
        int port = Integer.getInteger("port", 8080);

        router.post("/api").handler(request -> {
            flightService.makeBooking("BA123");
            request.response().end(System.getenv("HOSTNAME") + ": " + Thread.currentThread().getName()
                    + ":  Booking Count=" + flightService.getNumberOfBookings());
        });

        vertx.createHttpServer().requestHandler(router::accept).listen(port);
    }
}
