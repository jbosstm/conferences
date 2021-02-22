/**
 * JBoss, Home of Professional Open Source
 * Copyright 2016, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.redhat.developers.msa.aloha;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.impl.client.HttpClientBuilder;

import com.github.kennedyoliveira.hystrix.contrib.vertx.metricsstream.EventMetricsStreamHandler;

import feign.Logger;
import feign.Logger.Level;
import feign.httpclient.ApacheHttpClient;
import feign.hystrix.HystrixFeign;
import feign.opentracing.TracingClient;
import io.narayana.lra.annotation.CompensatorStatus;
import io.narayana.lra.client.LRAClient;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.contrib.spanmanager.DefaultSpanManager;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.impl.FutureFactoryImpl;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.spi.FutureFactory;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.JWTAuthHandler;
import io.vertx.ext.web.handler.StaticHandler;

public class AlohaVerticle extends AbstractVerticle {

    private static Tracer tracer = TracingConfiguration.tracer;
    private static String coordinatorHost = System.getProperty(LRAClient.CORRDINATOR_HOST_PROP, "lra-coordinator");
    private static Integer coordinatorPort = Integer.getInteger(LRAClient.CORRDINATOR_PORT_PROP, 8080);

    @Override
    public void start() throws Exception {
        Router router = Router.router(vertx);
        // Health Check
        router.get("/api/health").handler(ctx -> ctx.response().end("I'm ok"));

        // Tracing
        router.route().handler(TracingConfiguration::tracingHandler)
                .failureHandler(TracingConfiguration::tracingFailureHandler);

        router.route().handler(BodyHandler.create());
        router.route().handler(CorsHandler.create("*")
            .allowedMethods(new HashSet<>(Arrays.asList(HttpMethod.values())))
            .allowedHeader("Origin, X-Requested-With, Content-Type, Accept, Authorization"));

        // Aloha EndPoint
        // router.get("/api/aloha").handler(ctx -> ctx.response().end(aloha()));
        router.get("/api/aloha").handler(ctx -> aloha(ctx, (string) -> ctx.response()
                .end(string)));

        String keycloackServer = System.getenv("KEYCLOAK_AUTH_SERVER_URL");

        if (keycloackServer != null) {
            // Create a JWT Auth Provider
            JWTAuth jwt = JWTAuth.create(vertx, new JsonObject()
                .put("public-key",
                    "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEArfmb1i36YGxYxusjzpNxmw9a/+M40naa5RxtK826nitmWESF9XiXm6bHLWmRQyhAZluFK4RZDLhQJFZTLpC/w8HdSDETYGqnrP04jL3/pV0Mw1ReKSpzi3tIde+04xGuiQM6nuR84iRraLxtoNyIiqFmHy5pmI9hQhctfZNOVvggntnhXdt/VKuguBXqitFwGbfEgrJTeRvnTkK+rR5MsRDHA3iu2ZYaM4YNAoDbqGyoI4Jdv5Kl1LsP3qESYNeagRz6pIfDZWOoJ58p/TldVt2h70S1bzappbgs8ZbmJXg+pHWcKvNutp5y8nYw30qzU73pX6DW9JS936OB6PiU0QIDAQAB"));
            router.route("/api/aloha-secured").handler(JWTAuthHandler.create(jwt));
        }
        router.get("/api/aloha-secured").handler(ctx -> {
            User user = ctx.user();
            ctx.response().end("This is a secured resource. You're logged as " + user.principal().getString("name"));   
        });

        // Aloha Chained Endpoint
        router.get("/api/aloha-chaining").handler(ctx -> alohaChaining(ctx, (list) -> ctx.response()
            .putHeader("Content-Type", "application/json")
            .end(Json.encode(list))));

        router.put("/api/complete").handler(ctx -> complete(ctx));
        router.put("/api/compensate").handler(ctx -> compensate(ctx));
        router.get("/api/status").handler(ctx -> status(ctx));

        // Hystrix Stream Endpoint
        router.get(EventMetricsStreamHandler.DEFAULT_HYSTRIX_PREFIX).handler(EventMetricsStreamHandler.createHandler());

        // Static content
        router.route("/*").handler(StaticHandler.create());

        Integer port = Integer.getInteger("http.port", 8080);
        vertx.createHttpServer().requestHandler(router::accept).listen(port);
        System.out.println("Service running at 0.0.0.0:" + port);
    }

    private void aloha(RoutingContext ctx, Handler<String> resultHandler) {
        vertx.<String> executeBlocking(
            future -> {
            	boolean joint = joinLRA(ctx);

            	String alohaGreetings = aloha();
            	if(!joint) alohaGreetings += " (failed)"; 

                future.complete(alohaGreetings);
            },
            ar -> {
                // Back to the event loop
                String greetings = ar.result();
                resultHandler.handle(greetings);
            });
    }

    private String aloha() {
    	String hostname = System.getenv().getOrDefault("HOSTNAME", "unknown");
    	return String.format("Aloha mai %s", hostname);
    	
    }

    private void alohaChaining(RoutingContext ctx, Handler<List<String>> resultHandler) {
    	String lraUri = getLraId(ctx);

        vertx.<List<String>> executeBlocking(
            // Invoke the service in a worker thread, as it's blocking.
            future -> {
            	boolean joint = joinLRA(ctx);
            	List<String> greetings = new ArrayList<>();

            	String alohaGreeting = aloha();
            	if(!joint) alohaGreeting += " (failed)"; 
            	
            	greetings.add(alohaGreeting);
            	greetings.add(getNextService(ctx).bonjour(lraUri));

                future.complete(greetings);
            },
            ar -> {
                // Back to the event loop
                // result cannot be null, hystrix would have called the fallback.
                List<String> greetings = ar.result();
                // System.out.println("Passing back to caller: " + greetings);
                resultHandler.handle(greetings);
            });
    }

    private boolean joinLRA(RoutingContext ctx) {
        String lraUri = getLraId(ctx);
        String baseUri = getBaseUri(ctx.request().absoluteURI());

        System.out.printf("Joining LRA '%s' with baseUri '%s' on coordinator '%s:%s'%n",
                lraUri, baseUri, coordinatorHost, coordinatorPort);

        try (LRAClient lraClient = new LRAClient(coordinatorHost, coordinatorPort)) {
            String recoveryPath = lraClient.joinLRA(new URL(lraUri), 0L, baseUri, null);
            // System.out.println("Recovery path of the enlisted resource is: " + recoveryPath);
        } catch (Exception e) {
            System.err.printf("Can't join LRA %s, %s:%s", lraUri, e.getClass(), e.getMessage());
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private String getLraId(RoutingContext ctx) {
    	String lraHeader = ctx.request().getHeader(LRAClient.LRA_HTTP_HEADER);
    	if(lraHeader == null) lraHeader = ctx.request().getHeader(LRAClient.LRA_HTTP_HEADER.toLowerCase());
    	return lraHeader;
    }

    private void complete(RoutingContext ctx) {
        String lraId = sendResponse(ctx, "complete");
        System.out.printf("I was happy to say 'aloha' [%s completed]%n", lraId);
        ctx.response().end();
    }
    
    private void compensate(RoutingContext ctx) {
        String lraId= sendResponse(ctx, "compensate");
        System.out.printf("I'm sorry, I didn't mean the greetings 'aloha'. I'm taking it back [%s compensated]%n", lraId);
        ctx.response().end();
    }

    private void status(RoutingContext ctx) {
        sendResponse(ctx, "status");
        ctx.response().end(Json.encodePrettily(CompensatorStatus.Completed));
    }

    private String getBaseUri(String absUri) {
        Pattern pattern = Pattern.compile("^(.*/api).*", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(absUri);

        String baseUri;
        if(matcher.find()) {
            baseUri = matcher.group(1);
        } else {
            baseUri = absUri;
        }
        return baseUri;
    }

    private String sendResponse(final RoutingContext ctx, String endpointString) {
        String lraUri = ctx.request().getHeader(LRAClient.LRA_HTTP_HEADER);
        String uri = ctx.request().absoluteURI();
        String baseUri = getBaseUri(uri);
        
        String txId = LRAClient.getLRAId(lraUri);
        String responseUrl = String.format("%s/%s/%s", baseUri, txId, endpointString);

        ctx.response().putHeader("Content-type", "application/json; charset=utf-8");
        ctx.response().putHeader("Link", responseUrl);
        return lraUri;
    }

    /**
     * This is were the "magic" happens: it creates a Feign, which is a proxy interface for remote calling a REST endpoint with
     * Hystrix fallback support.
     *
     * @return The feign pointing to the service URL and with Hystrix fallback.
     */
    private BonjourService getNextService(RoutingContext context) {
        final Span serverSpan = context.get(TracingConfiguration.ACTIVE_SPAN);

        String host = System.getProperty("bonjour.host", "bonjour");
        String port = System.getProperty("bonjour.port", "8080");
        System.out.println(">>> Bonjour service expected being at " + host + ":" + port);
        
        return HystrixFeign.builder()
            // Use apache HttpClient which contains the ZipKin Interceptors
            .client(new TracingClient(new ApacheHttpClient(HttpClientBuilder.create().build()), tracer))
            // bind span to current thread
            .requestInterceptor((t) -> DefaultSpanManager.getInstance().activate(serverSpan))
            .logger(new Logger.ErrorLogger()).logLevel(Level.BASIC)
            .target(BonjourService.class, String.format("http://%s:%s/", host, port),
                (String lraUri) -> "Bonjour response (fallback)");
    }

}
