
## Using STM to scale applications

The functionality of the demonstration application is very simple: it maintains a count of the number of
bookings made by REST clients, ie the *actor* accepts http POST messages and modifies an internal
counter and reports the current value back to the client.

There are three versions of the same application:

1. [a standard vert.x application with no concurrency support](demo_without_STM/README.md)
2. [a standard vert.x application with STM support](demo_with_STM/README.md)
3. [a standard vert.x application with STM support running in a cloud environment](demo_with_STM_on_openshift/README.md)

First build the 3 versions of the demonstration application:


```bash
mvn clean package
```

and then try out each step starting with [a version without STM support](demo_without_STM/README.md) which
shows up issues due to lack of concurrency protection.
Then try out the [version that adds STM support](demo_with_STM/README.md).
And finally run a [version of the application](demo_with_STM_on_openshift/README.md) that scales by
running on more than one JVM.

The instructions for each demo assume you are in the same directory as the demo. If you run the
code from this directory just specify which pom to use (ie -f directory/pom.xml).

Notice that the cloud based version of the application requires some prerequisite steps
for installing the OpenShift cloud environment and these are detailed in
the [README for that step](demo_with_STM_on_openshift/README.md).
