package demo.demo12;

import demo.domain.TaxiService;
import demo.domain.TaxiServiceImpl;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.jboss.stm.Container;

/**
 * Demonstrates how to use volatile STM objects with verticles
 */
public class VolatileTaxiVerticle extends TaxiVerticle {
    private static int port = 8080;
    private static TaxiService service;
    private static Container<TaxiService> container;

    /**
     * STM initialization and verticle deployment
     */
    public static void main(String[] args) {
        container = new Container<>();

        service = container.create(new TaxiServiceImpl());
        initSTMMemory(service);

        DeploymentOptions opts = new DeploymentOptions().
                setInstances(10).
                setConfig(new JsonObject().put("port", port));

        Vertx.vertx().deployVerticle(VolatileTaxiVerticle.class.getName(), opts);
    }

    TaxiService getClone() {
        return container.clone(new TaxiServiceImpl(), service);
    }
}
