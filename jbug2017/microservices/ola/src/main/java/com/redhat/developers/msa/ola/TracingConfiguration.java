package com.redhat.developers.msa.ola;

import java.net.URISyntaxException;
import java.util.Collections;
import java.util.regex.Pattern;

import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

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
import io.narayana.lra.client.LRAClient;
import io.narayana.lra.client.LRAClientAPI;
import io.opentracing.NoopTracerFactory;
import io.opentracing.Tracer;
import io.opentracing.contrib.spring.web.autoconfig.WebTracingConfiguration;

/**
 * @author Pavol Loffay
 */
@Configuration
public class TracingConfiguration {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TracingConfiguration.class);
    private static final String SERVICE_NAME = "ola";

    @Autowired
    private Environment environment;

    @Bean
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


    @Bean
    public WebTracingConfiguration webTracingConfiguration() {
        return WebTracingConfiguration.builder()
                .withSkipPattern(Pattern.compile("/api/health"))
                .build();
    }

    /**
     *
     * This is were the "magic" happens: it creates a Feign, which is a proxy interface for remote calling a
     * REST endpoint with Hystrix fallback support.
     */
    @Bean
    public HolaService holaService(Tracer tracer) {
        // bind current span to Hystrix thread
        TracingConcurrencyStrategy.register();

        String host = System.getProperty("hola.host", "hola");
        String port = System.getProperty("hola.port", "8080");
        System.out.println(">>> Hola service expected being at " + host + ":" + port);

        return HystrixFeign.builder()
                .client(new TracingClient(new ApacheHttpClient(HttpClientBuilder.create().build()), tracer))
                .logger(new Logger.ErrorLogger()).logLevel(Logger.Level.BASIC)
                .decoder(new JacksonDecoder())
                .target(HolaService.class, String.format("http://%s:%s/", host, port),
                        (String lraId) -> Collections.singletonList("Hola response (fallback)"));
    }

    @Bean
    public LRAClientAPI lraClient() {
        try {
            int port = 8080;
            Integer portSys = Integer.getInteger(LRAClient.CORRDINATOR_PORT_PROP);
            String portEnv = environment.getProperty(LRAClient.CORRDINATOR_PORT_PROP);
            if(portSys != null) port = portSys;
            if(portSys == null && portEnv != null) port = Integer.parseInt(portEnv);

            String host = "lra-coordinator";
            String hostSys = System.getProperty(LRAClient.CORRDINATOR_HOST_PROP);
            String hostEnv = environment.getProperty(LRAClient.CORRDINATOR_HOST_PROP);
            if(hostSys != null) host = hostSys;
            if(hostSys == null && hostEnv != null) host = hostEnv;

            System.out.println(">>> LRA coordinator to connect is at " + host + ":" + port);
            return new LRAClient(host, port);
        } catch (URISyntaxException urise) {
            throw new IllegalStateException("Can't initalize a new LRA client", urise);
        }
    }
}
