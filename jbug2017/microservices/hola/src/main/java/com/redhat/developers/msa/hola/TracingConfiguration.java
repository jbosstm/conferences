package com.redhat.developers.msa.hola;
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

import java.util.Collections;
import java.util.EnumSet;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.apache.http.impl.client.HttpClientBuilder;

import com.uber.jaeger.metrics.Metrics;
import com.uber.jaeger.metrics.NullStatsReporter;
import com.uber.jaeger.metrics.StatsFactoryImpl;
import com.uber.jaeger.reporters.RemoteReporter;
import com.uber.jaeger.samplers.ProbabilisticSampler;
import com.uber.jaeger.senders.Sender;
import com.uber.jaeger.senders.UdpSender;

import feign.Logger;
import feign.httpclient.ApacheHttpClient;
import feign.hystrix.HystrixFeign;
import feign.jackson.JacksonDecoder;
import feign.opentracing.TracingClient;
import feign.opentracing.hystrix.TracingConcurrencyStrategy;
import io.opentracing.NoopTracerFactory;
import io.opentracing.Tracer;
import io.opentracing.contrib.web.servlet.filter.TracingFilter;

/**
 * This class uses CDI to alias Zipkin resources to CDI beans
 *
 */
public class TracingConfiguration {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TracingConfiguration.class);
    private static final String SERVICE_NAME = "hola";

    @Produces
    @Singleton
    public Tracer tracer() {
        String jaegerURL = System.getenv("JAEGER_SERVER_HOSTNAME");
        if (jaegerURL != null) {
            log.info("Using Jaeger tracer");
            return jaegerTracer(jaegerURL);
        }

        log.info("Using Noop tracer");
        return NoopTracerFactory.create();

    }

    private Tracer jaegerTracer(String url) {
        Sender sender = new UdpSender(url, 0, 0);
        return new com.uber.jaeger.Tracer.Builder(SERVICE_NAME,
                new RemoteReporter(sender, 100, 50,
                        new Metrics(new StatsFactoryImpl(new NullStatsReporter()))),
                new ProbabilisticSampler(1.0))
                .build();
    }

    /**
     * This is were the "magic" happens: it creates a Feign, which is a proxy interface for remote calling a REST endpoint with
     * Hystrix fallback support.
     *
     * @return The feign pointing to the service URL and with Hystrix fallback.
     */
    @Produces
    @Singleton
    private AlohaService alohaService(Tracer tracer) {
        // bind current span to Hystrix thread
        TracingConcurrencyStrategy.register();

        String host = System.getProperty("aloha.host", "aloha");
        String port = System.getProperty("aloha.port", "8080");
        System.out.println(">>> Aloha service expected being at " + host + ":" + port);

        return HystrixFeign.builder()
                // Use apache HttpClient which contains the ZipKin Interceptors
                .client(new TracingClient(new ApacheHttpClient(HttpClientBuilder.create().build()), tracer))

                // Bind Zipkin Server Span to Feign Thread
                .logger(new Logger.ErrorLogger()).logLevel(Logger.Level.BASIC)
                .decoder(new JacksonDecoder())
                .target(AlohaService.class, String.format("http://%s:%s/", host, port),
                        (String lraUri) -> Collections.singletonList("Aloha response (fallback)"));
    }

    @WebListener
    public static class TracingFilterRegistration implements ServletContextListener {
        @Inject
        private Tracer tracer;

        @Override
        public void contextInitialized(ServletContextEvent sce) {
            FilterRegistration.Dynamic filterRegistration = sce.getServletContext()
                    .addFilter("BraveServletFilter", new TracingFilter(tracer));
            // Explicit mapping to avoid trace on readiness probe
            filterRegistration.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), false, "/api/hola", "/api/hola-chaining");
        }

        @Override
        public void contextDestroyed(ServletContextEvent sce) {}
    }
}
