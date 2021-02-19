package cz.devconf2021.stm;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/stm")
@RequestScoped
public class LockpickingRestResource {
    ExecutorService executor;

    @ConfigProperty(name = "cz.devconf2021.stm.threadpool.size")
    int threadPoolSize;;

    @Inject
    LockpickingServiceFactory serviceFactory;

    @PostConstruct
    void postConstruct() {
        executor = Executors.newFixedThreadPool(threadPoolSize);
    }

    @PreDestroy
    void preDestroy() {
        executor.shutdown();
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public CompletionStage<String> actionNumber() {
        return CompletableFuture.supplyAsync(
                () -> getInfo(serviceFactory.getInstance()),
                executor);
    }

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public CompletionStage<String> doAction() {
        return CompletableFuture.supplyAsync(() -> {
            LockpickingService lockpickingService = serviceFactory.getInstance();

            lockpickingService.doAction();

            return getInfo(lockpickingService);
        }, executor);
    }

    private String getInfo(LockpickingService lockpickingService) {
        return Thread.currentThread().getName()
                + ":  Action Number = " + lockpickingService.getActionNumber();
    }
}
