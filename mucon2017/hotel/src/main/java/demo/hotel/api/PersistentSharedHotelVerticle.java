package demo.hotel.api;

import com.arjuna.ats.arjuna.common.Uid;
import demo.common.api.PersistentSharedBookingVerticle;
import demo.hotel.domain.HotelService;
import demo.hotel.domain.HotelServiceImpl;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.jboss.stm.Container;

import java.io.IOException;
import java.util.function.Supplier;

/**
 * Demonstrates how to use persistent STM objects with verticles
 */
public class PersistentSharedHotelVerticle extends PersistentSharedBookingVerticle {
    private static String SERVICE_NAME = "hotel";

    private static Container<HotelService> container;
    private static String uid;

    public PersistentSharedHotelVerticle() {
        super(SERVICE_NAME);
    }

    /**
     * STM initialization and verticle deployment
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        container = new Container<>(Container.TYPE.PERSISTENT, Container.MODEL.SHARED);

        Supplier<String> uidCreator = PersistentSharedHotelVerticle::createUid;

        uid = initSharedState(SERVICE_NAME, uidCreator);

        DeploymentOptions opts = new DeploymentOptions().
                setInstances(8).
                setConfig(new JsonObject().put("port", port));

        Vertx.vertx().deployVerticle(PersistentSharedHotelVerticle.class.getName(), opts);
    }

    private static String createUid() {
        HotelService service = container.create(new HotelServiceImpl());

        uid = container.getIdentifier(service).toString();

        initSTMMemory(service);

        return uid;
    }

    protected HotelService getClone() {
        return container.clone(new HotelServiceImpl(), new Uid(uid));
    }
}
