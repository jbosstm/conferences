
## A simple vert.x application without concurrency support

The first version of the demonstration code starts 8 instances of the verticle each listening
for POST requests on a http endpoint. The vert.x runtime manages incoming connection requests
and distributes them in a round-robin fashion to the connection handlers of each of the verticle
instances.
This means that application can concurrently service multiple requests (according to the
number of instance deployed which you can change via the
[main method of the MainVerticle](src/main/java/io/narayana/mucon/MainVerticle.java#L14)).

Start the application running (make sure you are in the appropriate directory for the
demonstartion) using the vertx run plugin (you will already have compiled
the example in the previous step):

```bash
mvn vertx:run
```

You can test that the application services requests in different threads/verticles by issuing
two requests:

```
$ curl -X POST http://localhost:8080/api
vert.x-eventloop-thread-0:  Booking Count=1
$ curl -X POST http://localhost:8080/api
vert.x-eventloop-thread-1:  Booking Count=2
```

The responses indicate the name of the thread that handled the response. Notice that the second
response indicates that the request was serviced on a different thread than the first request.

In this first version there no concurrency support. We stress test the application to show
why this is a problem (with a view to solving the issue using STM in the next version):

```bash
  java -jar ../stress/target/mucon-stress-1.0.jar requests=100 parallelism=50 url=/api
```

This command uses 50 threads each sending 100 POST requests to the running application
so one would expect 5000 requests in total.
If there are no failures you should see the following output:

```
Waiting for 5000 requests
0 out of 5000 requests failed in 1598 ms
```

If there were no concurrency issues the booking count would be 5000 but since there is no
concurrency support we expect some interleaving so the count ought to be less than 5000.
For example, issue another POST request to get the current count:

```
curl -X POST http://localhost:8080/api
vert.x-eventloop-thread-0:  Booking Count=4993
```

The value you see is likely to be different from 4993 but the point is that is will probable
be less than 5000.

In the [next step](../demo_with_STM/README.md) you will fix the concurrecy issue using STM

