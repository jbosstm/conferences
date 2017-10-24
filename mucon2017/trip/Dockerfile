FROM fabric8/java-jboss-openjdk8-jdk:1.2.6
EXPOSE 8080
COPY target/*.jar /vertx/
WORKDIR /vertx
CMD java -jar *.jar -Dvertx.cacheDirBase=/tmp -DObjectStoreEnvironmentBean.objectStoreDir=/vertx/data 

