# ola
ola microservice using Spring Boot

The detailed instructions to run *Red Hat Helloworld MSA* demo, can be found at the following repository: <https://github.com/redhat-helloworld-msa/helloworld-msa>


Build and Deploy ola locally
----------------------------

1. Open a command prompt and navigate to the root directory of this microservice.
2. Type this command to build and execute the microservice:

        mvn clean compile spring-boot:run

3. The application will be running at the following URL: <http://localhost:8080/api/ola>


Deploy the application in OpenShift
-----------------------------------

1. Make sure to be connected to the OpenShift
2. Execute

		mvn package fabric8:deploy

Running with LRA
----------------

Coordinator needs to  be started

       java -jar target/lra-coordinator-swarm.jar -Dswarm.port.offset=100

To start this ola MSA

       mvn clean package && java -Dlra.http.host=localhost -Dlra.http.port=8180 -Dhola.port=8282 -Dhola.host=localhost -jar target/ola.jar --server.port=8181

To show trace/debug messages for the Spring Boot: `--trace` or `--debug`

To debug with agent:

       java -agentlib:jdwp=transport=dt_socket,address=8787,server=y,suspend=n -Dlra.http.host=localhost -Dlra.http.port=8180 -Dhola.port=8282 -Dhola.host=localhost -jar target/ola.jar --server.port=8181

Curl testing

       curl -i -X GET http://localhost:8181/api/ola-chaining/
