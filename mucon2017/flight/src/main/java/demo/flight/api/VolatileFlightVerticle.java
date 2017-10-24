package demo.flight.api;

import demo.common.api.BookingVerticle;
import demo.flight.domain.FlightService;
import demo.flight.domain.FlightServiceImpl;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.jboss.stm.Container;

/**
 * Demonstrates how to use volatile STM objects with verticles
 */
public class VolatileFlightVerticle extends BookingVerticle {
    private static final String SERVICE_NAME = "flight";

    private static FlightService service;
    private static Container<FlightService> container;

    public VolatileFlightVerticle() {
        super(SERVICE_NAME);
    }

    /**
     * STM initialization and verticle deployment
     */
    public static void main(String[] args) {
        int port = Integer.getInteger("port", 8080);

        container = new Container<>(); // default is RECOVERABLE and EXCLUSIVE - ie not persistent and not shared between processes

        service = container.create(new FlightServiceImpl());
        initSTMMemory(service);

        DeploymentOptions opts = new DeploymentOptions().
                setInstances(10).
                setConfig(new JsonObject().put("port", port));

        Vertx.vertx().deployVerticle(VolatileFlightVerticle.class.getName(), opts);
    }

    protected FlightService getClone() {
        return container.clone(new FlightServiceImpl(), service);
    }
}
