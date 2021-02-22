package com.redhat.developers.msa.aloha;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.uber.jaeger.metrics.Metrics;
import com.uber.jaeger.metrics.NullStatsReporter;
import com.uber.jaeger.metrics.StatsFactoryImpl;
import com.uber.jaeger.reporters.RemoteReporter;
import com.uber.jaeger.samplers.ProbabilisticSampler;
import com.uber.jaeger.senders.Sender;
import com.uber.jaeger.senders.UdpSender;

import io.opentracing.NoopTracerFactory;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMap;
import io.opentracing.tag.Tags;
import io.vertx.ext.web.RoutingContext;

/**
 * @author Pavol Loffay
 */
public class TracingConfiguration {
    private static final String SERVICE_NAME = "aloha";

    public static final String ACTIVE_SPAN = AlohaVerticle.class + ".activeSpan";
    public static final Tracer tracer = tracer();

    private TracingConfiguration() {}

    private static Tracer tracer() {
        String jaegerURL = System.getenv("JAEGER_SERVER_HOSTNAME");
        if (jaegerURL != null) {
            System.out.println("Using Jaeger tracer");
            return jaegerTracer(jaegerURL);
        }


        System.out.println("Using Noop tracer");
        return NoopTracerFactory.create();
    }

    private static Tracer jaegerTracer(String url) {
        Sender sender = new UdpSender(url, 0, 0);
        return new com.uber.jaeger.Tracer.Builder(SERVICE_NAME,
                new RemoteReporter(sender, 100, 50,
                        new Metrics(new StatsFactoryImpl(new NullStatsReporter()))),
                new ProbabilisticSampler(1.0))
                .build();
    }

    public static void tracingHandler(RoutingContext routingContext) {
        SpanContext parent = tracer.extract(Format.Builtin.HTTP_HEADERS, new TextMap() {
            @Override
            public Iterator<Map.Entry<String, String>> iterator() {
                return routingContext.request().headers().iterator();
            }
            @Override
            public void put(String key, String value) {}
        });

        Span span = tracer.buildSpan(routingContext.request().method().toString())
                .asChildOf(parent)
                .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_SERVER)
                .withTag(Tags.HTTP_METHOD.getKey(), routingContext.request().method().toString())
                .withTag(Tags.HTTP_URL.getKey(), routingContext.request().absoluteURI())
                .withTag(Tags.COMPONENT.getKey(), "vertx")
                .start();

        routingContext.put(ACTIVE_SPAN, span);

        routingContext.addBodyEndHandler(event -> {
            Tags.HTTP_STATUS.set(span, routingContext.response().getStatusCode());
            span.finish();
        });

        routingContext.next();
    }

    public static void tracingFailureHandler(RoutingContext routingContext) {
        if (routingContext.failed() == true) {
            Span span = routingContext.get(ACTIVE_SPAN);
            Tags.ERROR.set(span, Boolean.TRUE);

            if (routingContext.failure() != null) {
                Map<String, Object> errorLogs = new HashMap<>(2);
                errorLogs.put("event", Tags.ERROR.getKey());
                errorLogs.put("error.object", routingContext.failure());
                span.log(errorLogs);
            }
        }
        routingContext.response().setStatusCode(404).end();
    }
}
