package demo.hotel.api;

import demo.common.api.BookingVerticle;
import demo.hotel.domain.HotelService;
import demo.hotel.domain.HotelServiceImpl;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.jboss.stm.Container;

/**
 * Demonstrates how to use volatile STM objects with verticles
 */
public class VolatileHotelVerticle extends BookingVerticle {
    private static final String SERVICE_NAME = "hotel";

    private static HotelService service;
    private static Container<HotelService> container;

    public VolatileHotelVerticle() {
        super(SERVICE_NAME);
    }

    /**
     * STM initialization and verticle deployment
     */
    public static void main(String[] args) {
        int port = Integer.getInteger("port", 8080);

        container = new Container<>(); // default is RECOVERABLE and EXCLUSIVE - ie not persistent and not shared between processes

        service = container.create(new HotelServiceImpl());
        initSTMMemory(service);

        DeploymentOptions opts = new DeploymentOptions().
                setInstances(10).
                setConfig(new JsonObject().put("port", port));

        Vertx.vertx().deployVerticle(VolatileHotelVerticle.class.getName(), opts);
    }

    protected HotelService getClone() {
        return container.clone(new HotelServiceImpl(), service);
    }
}
