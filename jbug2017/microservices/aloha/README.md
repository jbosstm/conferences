# aloha
Aloha microservice using Vert.X

The detailed instructions to run *Red Hat Helloworld MSA* demo, can be found at the following repository: <https://github.com/redhat-helloworld-msa/helloworld-msa>


Build and Deploy aloha locally
------------------------------

1. Open a command prompt and navigate to the root directory of this microservice.
2. Type this command to build and execute the service:

        mvn clean compile exec:java

3. The application will be running at the following URL: <http://localhost:8080/api/aloha>

Deploy the application in Openshift using Fabric8 plugin
---------------------------------------------------------

1. Make sure to be connected to the Docker Daemon
2. Execute

		mvn clean package docker:build fabric8:json fabric8:applyjava -jar

Running with LRA
----------------

To run

		mvn clean package && java -Dbonjour.host=localhost -Dbonjour.port=8484 -Dhola.host=localhost -Dhola.port=8282 -Dola.host=localhost -Dola.port=8181 -Daloha.host=localhost -Daloha.port=8383 -Dlra.http.host=localhost -Dlra.http.port=8180 -jar target/api-gateway.jar

To run with debug		
		
		java -agentlib:jdwp=transport=dt_socket,address=8787,server=y,suspend=n -jar target/aloha-fat.jar -Dhttp.port=8383 -Dbonjour.host=localhost -Dbonjour.port=8484 -Dlra.http.port=8180

To get more logging
		
        java -Djava.util.logging.config.file=/opt/minishift/msa-helloworld/sources/aloha/logging.config -jar target/aloha-fat.jar -Dhttp.port=8383 -Dbonjour.host=localhost -Dbonjour.port=8484 -Dlra.http.port=8180
