
## A simple Vert.x application with STM support

This version builds on the [version without STM support](../demo_without_STM/README.md).

Firstly we make the flight service transactional by annotating the interface with the
@Transactional and @NestedTopLevel annotations
[as shown in the java interface](src/main/java/io/narayana/devconf/FlightService.java).

By default all methods mutate state. To achieve better throughput we can explicitly mark
up which methods change state and which ones only read it by using the @WriteLock and
@ReadLock annotations, respectively. You can see how we have annotated the example service by
[looking at the source code of the interface implementation class](src/main/java/io/narayana/devconf/FlightServiceImpl.java).

Start the application running using the vertx run plugin:

```bash
mvn vertx:run
```

Now repeat the stress test you performed in the first version of the application:

```bash
  java -jar ../stress/target/devconf-stress-1.0.jar requests=100 parallelism=50 url=/api
```

Recall that previously the resulting number of bookings was less than 5000. But now that we
are using STM the count is expected to be 5000. Issue one more request to verify that:

```
$ java -jar ../stress/target/devconf-stress-1.0.jar requests=100 parallelism=50 url=/api 
Waiting for 5000 requests
0 out of 5000 requests failed in 1619 ms
$ curl -X POST http://localhost:8080/api                                  
vert.x-eventloop-thread-7:  Booking Count=5001
```

In the [next and final version of the demo](../demo_with_STM_on_openshift/README.md) we show how
to run the same application spread over multiple JVMs.
