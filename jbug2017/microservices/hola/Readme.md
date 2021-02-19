# hola
hola microservice using Java EE (JAX-RS) on WildFly Swarm

The detailed instructions to run *Red Hat Helloworld MSA* demo, can be found at the following repository: <https://github.com/redhat-helloworld-msa/helloworld-msa>


Build and Deploy hola locally
-----------------------------

1. Open a command prompt and navigate to the root directory of this microservice.
2. Type this command to build and execute the application:

        mvn wildfly-swarm:run

3. This will create a uber jar at  `target/hola-swarm.jar` and execute it.
4. The application will be running at the following URL: <http://localhost:8080/api/hola>

Deploy the application in Openshift
-----------------------------------

1. Make sure to be connected to the Openshift cluster
2. Execute

	mvn package fabric8:deploy

Running with LRA
----------------

One liner to compile and run

    mvn clean package && java -jar target/hola-swarm.jar -Dswarm.http.port=8282 -Dlra.http.port=8180 -Daloha.host=localhost -Daloha.port=8383

For testing purposes a `curl` command

    curl -i -X GET http://localhost:8282/api/hola-chaining/

Handy parameters of the LRA

- change the host where lra coordinator resides
    -Dlra.http.host=localhost
- change the port to say where lra coordinator resides
    -Dlra.http.port=8080
- change what swarm is logging
    -Dswarm.logging=TRACE
- port of undertow is sitting at
    -Dswarm.http.port=8181
- port offset for the swarm instance
    -Dswarm.port.offset=100
- what interface the swarm bind to (0.0.0.0 is default)
    -Dswarm.bind.address
- debugging
    -agentlib:jdwp=transport=dt_socket,address=8787,server=y,suspend=n
  used as
    java -agentlib:jdwp=transport=dt_socket,address=8787,server=y,suspend=n -jar target/hola-swarm.jar


