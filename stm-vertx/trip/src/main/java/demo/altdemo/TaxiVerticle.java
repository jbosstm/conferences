package demo.altdemo;

import com.arjuna.ats.arjuna.AtomicAction;
import demo.domain.Booking;
import demo.domain.ServiceResult;
import demo.domain.TaxiService;
import demo.domain.TaxiServiceImpl;
import demo.util.ProgArgs;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.jboss.stm.Container;

public class TaxiVerticle extends AbstractVerticle {
    private static ProgArgs options;
    private static int port = 8084;

    private TaxiService serviceClone;
    private static TaxiService service;
    private static Container<TaxiService> container;

    public static void main(String[] args) {
        deployService(args);
    }

    public static String deployService(String[] args) {
        options = new ProgArgs(args);
        port = options.getIntOption("taxi.port", port);
        container = new Container<>(Container.TYPE.RECOVERABLE, Container.MODEL.EXCLUSIVE);

        service = container.create(new TaxiServiceImpl());
        initSTMMemory(service);

        DeploymentOptions opts = new DeploymentOptions().
                setInstances(10).
                setConfig(new JsonObject().put("name", "demo12").put("taxi.port", port));

        Vertx.vertx().deployVerticle(TaxiVerticle.class.getName(), opts);

        return container.getIdentifier(service).toString();
    }

    public TaxiVerticle() {
    }

    @Override
    public void start(Future<Void> future) throws Exception {
        serviceClone = container.clone(new TaxiServiceImpl(), service);

        startServer(future, config().getInteger("taxi.port"));
    }

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

        System.out.printf("%s service listening on http://localhost:%d%s%n" , getServiceName(), listenerPort, route1);
    }

    private void getRoutes(Router router) {
        router.get(String.format("/api/%s", getServiceName())).handler(this::getBookings);
        router.post(String.format("/api/%s/:seats", getServiceName())).handler(this::makeBooking);
    }

    private String getServiceName() {
        return "taxi";
    }

    private void getBookings(RoutingContext routingContext) {
        try {
            AtomicAction A = new AtomicAction();

            A.begin();
            int activityCount = serviceClone.getBookings(); // done as a sub transaction of A since mandatory is annotated wiht @Nested
            A.commit();

            System.out.printf("%s: cnt: %d%n", getServiceName(), serviceClone.getBookings());
            routingContext.response()
                    .setStatusCode(201)
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end(Json.encodePrettily(new ServiceResult(getServiceName(), Thread.currentThread().getName(), activityCount)));
        } catch (Exception e) {
            routingContext.response()
                    .setStatusCode(406)
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end(new JsonObject().put("Status", e.getMessage()).encode());
        }
    }

    private void makeBooking(RoutingContext routingContext) {
        try {
            AtomicAction A = new AtomicAction();

            A.begin();
            serviceClone.bookTaxi(); // done as a sub transaction of A since mandatory is annotated wiht @Nested
            int activityCount = serviceClone.getBookings();
            A.commit();

            routingContext.response()
                    .setStatusCode(201)
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end(Json.encodePrettily(new ServiceResult(getServiceName(), Thread.currentThread().getName(), activityCount)));
        } catch (Exception e) {
            routingContext.response()
                    .setStatusCode(406)
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end(new JsonObject().put("Status", e.getMessage()).encode());
        }
    }

    // workaround for JBTM-1732
    private static void initSTMMemory(Booking service) {
        AtomicAction A = new AtomicAction();

        A.begin();
        service.init();
        A.commit();
    }
}
