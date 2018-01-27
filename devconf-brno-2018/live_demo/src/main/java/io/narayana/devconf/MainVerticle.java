package io.narayana.devconf;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import org.jboss.stm.Container;

public class MainVerticle extends AbstractVerticle {
//    private static Container<FlightService> container;
    private static FlightService flightService;

    public static void main(String[] args) {
//        container = new Container<>();
        flightService = new FlightServiceImpl(); //container.create(new FlightServiceImpl());

        Vertx.vertx().deployVerticle(MainVerticle.class.getName(), new DeploymentOptions().setInstances(1));
    }

    @Override
    public void start() {
//        FlightService clone = container.clone(new FlightServiceImpl(), flightService);
        Router router = Router.router(vertx);

        router.post("/api").handler(request -> {
            flightService.makeBooking("BA123");
            request.response().end(Thread.currentThread().getName()
                    + ":  Booking Count=" + flightService.getNumberOfBookings());
        });

        vertx.createHttpServer().requestHandler(router::accept).listen(8080);
    }

}
