
# Scaling out with more JVMs using OpenShift

This demo builds on [the STM version of the demo](../demo_with_STM/README.md) by making the
STM flight domain object persistent. Doing
this means that the object can be shared between JVMs. Although it does not perform as well
as volatile STM it does show that the same service can be used in both environments with only
minor changes.

The main difference is how the STM container is constructed: instead of using the default
volatile unshared container [we create it to have type PERSISTENT and model SHARED](src/main/java/io/narayana/devconf/Helper.java#L22).

We will use OpenShift for this part of the demo but if you want to do a quick test (or wish
to skip the install of OpenShift) you can see it working locally by typing:

```bash
mvn compile exec:java -Ppersistent
```

This command will start a Vert.x application in a single JVM which listens on the endpoint http://localhost:8080/api waiting for http POST requests:

```
[INFO] Scanning for projects...
[INFO]                                                                         
[INFO] ------------------------------------------------------------------------
[INFO] Building stm-vertx-demo-flight 1.0
[INFO] ------------------------------------------------------------------------
[INFO] 
[INFO] --- exec-maven-plugin:1.5.0:java (default-cli) @ stm-vertx-demo-flight ---
DEPLOYING PersistentMainVerticle
Nov 23, 2017 3:18:35 PM com.arjuna.common.util.propertyservice.AbstractPropertiesFactory getPropertiesFromFile
WARN: ARJUNA048002: Could not find configuration file, URL was: null
Nov 23, 2017 3:18:35 PM com.arjuna.ats.arjuna.recovery.TransactionStatusManager start
INFO: ARJUNA012170: TransactionStatusManager started on port 39979 and host 127.0.0.1 with service com.arjuna.ats.arjuna.recovery.ActionStatusService
Created uid 0:ffffc0a80008:884d:5a16e6cb:1
Using uid 0:ffffc0a80008:884d:5a16e6cb:1
Using uid 0:ffffc0a80008:884d:5a16e6cb:1
```

Two different instances of the verticle share the same STM object which is uniquely identified in this example as

```
Created uid 0:ffffc0a80008:884d:5a16e6cb:1
```

The output

```
Using uid 0:ffffc0a80008:884d:5a16e6cb:1
Using uid 0:ffffc0a80008:884d:5a16e6cb:1
```

indicates that the two instances are sharing the same object.

To verify that all is well issue an http POST request:

```bash
curl -X POST http://localhost:8080/api
```

which should produce output similar to:

```
dev1.ncl.jboss.com: vert.x-eventloop-thread-0:  Booking Count=1
```

The first two fields of the response show the host and thread where the request was handled.
Note also that if you kill and restart the Vert.x application and reissue the POST request
you should observe that the old booking count was remembered, as it should for a persistent
STM object.

## Running on OpenShift (a Container Application Platform by Red Hat)

### Install minishift

minishift is a single-node OpenShift cluster running inside a VM.

First you need to [download and install minishift](https://docs.openshift.org/latest/minishift/getting-started/installing.html). After unpacking the binaries for your chosen platform. Make sure
the path to the install location is in the run path. minishift needs to run inside a hypervisor
and in the demo instructions below we assume [VirtualBox](https://www.virtualbox.org/wiki/Downloads) running on linux.

# minishift may require authenticated access to your rep
# https://github.com/settings/tokens
MINISHIFT_GITHUB_API_TOKEN="your github ssh key"

https://developer.github.com/v3/#increasing-the-unauthenticated-rate-limit-for-oauth-applications

https://docs.openshift.org/latest/minishift/getting-started/quickstart.html
https://www.if-not-true-then-false.com/2010/install-virtualbox-with-yum-on-fedora-centos-red-hat-rhel/


Start minishift, login and create a new project and a PVC (Persistent Volume Claim) and deploy
the app:

```bash
$ minishift start --vm-driver=virtualbox # or whatever hypervisor you are using
$ minishift console # opens the openshift web console
$ oc login -u developer -p developer
$ oc new-project stmdemo
```

If minishift fails to start with a rate limit error you may need to create a github oauth token: 
create one at https://github.com/settings/tokens and use the generated token:
export MINISHIFT_GITHUB_API_TOKEN="github oauth token"

The easiest way to create a "Persistent Volume Claim" is via the OpenShift console (give it
the name "stm-vertx-demo-flight-logs" with capacity 1GiB and RWX (Read-Write-Many) Access Mode.
We use the same name as the
maven artifact id for the demo to simplify the configuration of OpenShift deployments and routes.
For more details look at the yaml files in the [sources directory](src/main/fabric8).

### Build and deploy the Vert.x application to OpenShift

There is a fabric8 plugin that simplifies OpenShift deployments:

```bash
$ mvn fabric8:deploy -Popenshift
```

This plugin builds a docker image of the application and deploys it to the running OpenShift
cluster.

You should see the [expected output](deploy-output.txt) from the deploy command in your
command terminal.

You can monitor the progress of the deployment via the openshif console (which you started
above). You can also interact with OpenShift [Using the OpenShift Client Binary (oc)](https://docs.openshift.org/latest/minishift/openshift/openshift-client-binary.html).

Now check that the service/application is running by sending a POST request. You can find
the host name that the service is listening on using the OpenShift client tools or the console:

```
[mmusgrov@dev1 demo_with_STM_on_openshift](devconf)$ oc get routes
NAME                    HOST/PORT                                             PATH      SERVICES                PORT      TERMINATION   WILDCARD
stm-vertx-demo-flight   stm-vertx-demo-flight-stmdemo.192.168.99.100.nip.io             stm-vertx-demo-flight   http                    None
```

So, with that HOST/PORT combination (which is typical for a local minishift installation), issue a POST request:

```bash
curl -X POST http://stm-vertx-demo-flight-stmdemo.192.168.99.100.nip.io/api
```

and expect output similar to:

> stm-vertx-demo-flight-5-tr5zr: vert.x-eventloop-thread-0:  Booking Count=1

Each time you POST a request the booking count should increment.

To demonstrate sharing of the STM memory try scaling up the number of pods used to run the
application via the OpenShift console. Then reissue the POST requests and notice that the
hostId field (stm-vertx-demo-flight-5-tr5zr in this example) changes as OpenShift load
balances the requests between the pods.

To demonstrate the persistence aspect of STM try scaling down to zero and back up again.
Notice that the booking count is still the same as before (you scaled down to and back up
from zero).

