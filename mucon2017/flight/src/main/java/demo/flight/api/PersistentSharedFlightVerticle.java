package demo.flight.api;

import com.arjuna.ats.arjuna.common.Uid;
import demo.common.api.PersistentSharedBookingVerticle;
import demo.flight.domain.FlightService;
import demo.flight.domain.FlightServiceImpl;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.jboss.stm.Container;

import java.io.IOException;
import java.util.function.Supplier;

/**
 * Demonstrates how to use persistent STM objects with verticles
 */
public class PersistentSharedFlightVerticle extends PersistentSharedBookingVerticle {
    private static String SERVICE_NAME = "flight";

    private static Container<FlightService> container;
    private static String uid;

    public PersistentSharedFlightVerticle() {
        super(SERVICE_NAME);
    }

    /**
     * STM initialization and verticle deployment
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        container = new Container<>(Container.TYPE.PERSISTENT, Container.MODEL.SHARED);

        Supplier<String> uidCreator = PersistentSharedFlightVerticle::createUid;

        uid = initSharedState(SERVICE_NAME, uidCreator);

        DeploymentOptions opts = new DeploymentOptions().
                setInstances(8).
                setConfig(new JsonObject().put("port", port));

        Vertx.vertx().deployVerticle(PersistentSharedFlightVerticle.class.getName(), opts);
    }

    private static String createUid() {
        FlightService service = container.create(new FlightServiceImpl());

        uid = container.getIdentifier(service).toString();

        initSTMMemory(service);

        return uid;
    }

    protected FlightService getClone() {
        return container.clone(new FlightServiceImpl(), new Uid(uid));
    }
}
