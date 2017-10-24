
## The steps that I used for the demonstration were as follows

For the live coding demo:

Demo 1: Scaling up with more threads, step 1 without STM support

```bash
mkdir demo; cd demo
mvn io.fabric8:vertx-maven-plugin:1.0.7:setup -DvertxVersion=3.4.2 -Ddependencies=web
```
navigate to the verticles start method in an IDE
add a vertx route and create an http server and test:
```bash
mvn compile vertx:run
```
NB the vertx run goal will recompile and redeploy if it detects source changes
BUT if you change the pom you must restart the run plugin.

*The next three steps are in the directory demo_without_STM*

create a flightService
add a main: construct flightService and deploy the verticle (update launcher prop in pom)
add more instances of the vertcle in the Vertx.vertx().deployVerticle step
stress test to show concurrency issues using:

```bash
  java -jar stress/target/stress-1.0.jar requests=100 parallelism=50 url=/api
```

highlight the concurency issue with the flight booking count

Demo 1 ctd: step 2 adds STM support

add STM support to fix the issue
  add dependency on org.jboss.narayana.stm and logging to the pom
  reimport the pom in the IDE
  add STM annotations to the service interface and implementation
  create an STM container and objects in the main method
    - container.create(new FlightServiceImpl());
  clone it in the route handler
    - container.clone(new FlightServiceImpl(), flightService);
  recompile mvn compile vertx:run

rerun the stress test to show that there are no concurrency issues:
```bash
  java -jar stress/target/stress-1.0.jar requests=100 parallelism=50 url=/api
```

# Scaling out with more JVMs using OpenShift

Now show how to share STM across JVMs
  make the container type PERSISTENT and model SHARED
  (NB need to prime the object to force it into the store)

Start minishift, login and create a new project and a PVC and deploy the app:

```bash
> minishift start --vm-driver=virtualbox # or whatever hypervisor you are using
> minishift console # opens the openshift web console
> oc login -u developer -p developer
> oc new-project stmdemo
```

Create a "Persistent Volume Claim" via the OpenShift console (give it the name "stm-vertx-demo-flight-logs", capacity 1GiB and RWX (Read-Write-Many) Access Mode.

```bash
> mvn fabric8:deploy -Popenshift -f flight/pom.xml
> curl -X POST http://stm-vertx-demo-flight-stmdemo.192.168.99.100.nip.io/api/flight/BA123
```

scale up via the OpenShift console (pointing out the hostId field)
scale down to zero and back up (pointing out that the booking count was persisted)

## And the following provides more details and provides some other examples you can try out:

### Adding more threads to service a workload

```bash
mvn exec:java -Pvolatile -Dport=8080 -f flight/pom.xml

curl -X POST http://localhost:8080/api/flight/BA123
```

Observe that each time the request is issued it is serviced by a different thread (watch the threadId field change).

### Adding more JVMs to service a workload:

This demo shows how different JVMs can share the same transactional memory. We show how
to run and scale the services using the minishift container platform.

Install minishift:

Download the [relevant binary from](https://github.com/minishift/minishift/releases) and add
the minishift executable to the path, on linux for example

```bash
INSTDIR=~/products/openshift/minishift/minishift-1.7.0-linux-amd64
export PATH=<install location>/minishift-1.7.0-linux-amd64:$PATH
minishift start --vm-driver=virtualbox # or whatever hypervisor you are using
minishift console # opens the openshift web console
oc login -u developer -p developer
oc new-project stmdemo
```

The service that we are about to deploy to minishift uses persistant storage so that any data
survives if a pod crashes. It also needs to be shared by different pods so that the transactional
memory can be shared between different JVMs. Go to the minishift console and create a
"Persistent Volume Claim" by clicking on the "Storage" menu option on the left hand pane of the console.

Give it the name "stm-vertx-demo-flight-logs", capacity 1GiB and RWO (Read-Write-Once) Access Mode.
Now deploy the service:

```bash
mvn fabric8:deploy -Popenshift -f flight/pom.xml
```

or use OpenShift online:

```bash
https://manage.openshift.com/account/index
create the storage stm-vertx-demo-flight-logs
oc login -u rhn-engineering-mmusgrov --server=https://console.starter-us-west-1.openshift.com
mvn fabric8:deploy -Popenshift -f flight/pom.xml
```

and create a flight booking:

```bash
curl -X POST http://stm-vertx-demo-flight-stmdemo.192.168.99.100.nip.io/api/flight/BA123
```

If you periodically query the flight status you should see it being serviced on different threads
[just as in the earlier demo](adding-more-threads-to-service-a-workload):

```bash
curl -X GET  http://stm-vertx-demo-flight-stmdemo.192.168.99.100.nip.io/api/flight
```

Now go back to the console, locate the flight deployment (Deployments -> stm-vertx-demo-flight -> #1)
and scale up the number of pods to 2.
Now create some more flight bookings and observe that the requests are serviced by different pods (watch the hostId field change).


### Composing STM objects

start the trip verticle which manages theatre, taxi and train bookings:
```bash
mvn clean compile exec:java -Ptrip -f trip/pom.xml
```
book a theatre and taxi:
```bash
curl -X POST http://localhost:8080/api/trip/Apollo/TaxiFirm
```
book a theatre and taxi. Abort the taxi and book a train instead:
```bash
curl -X POST http://localhost:8080/api/trip/Apollo/fail_TaxiFirm/train
```
book a theatre and taxi. Abort the theatre booking and observe that the taxi is cancelled
```bash
curl -X POST http://localhost:8080/api/trip/Apollofail/XYZ
```

@Nested
  starts a nest txn which commits after the method exits (NB it never aborts)

@TopLevel
  starts a new txn which commits after the method exits (NB it never aborts)

### Using openshift online:

https://manage.openshift.com/account/index

https://api.starter-us-west-1.openshift.com/oauth/authorize?client_id=openshift-web-console&response_type=code&state=eyJ0aGVuIjoiLyIsIm5vbmNlIjoiMTUwOTY0MTM3MDY2NS0zNDg2OTA1Njc5MjYzOTA0OTAyOTE4ODc2ODM3NTQxMDEwNTg5MzMyMTg0MTIzOTg0MTEwMDEzNzgzMDM3Mjg4NTQwMjgxMzU4MTA3ODc5In0&redirect_uri=https%3A%2F%2Fconsole.starter-us-west-1.openshift.com%2Fconsole%2Foauth

