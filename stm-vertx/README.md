

cd /home/mmusgrov/source/forks/narayana/conferences/stm-vertx
export PATH=/home/mmusgrov/products/openshift/minishift2:$PATH 
virtualbox
minishift start
minishift console
oc login -u developer -p developer
oc new-project stmdemo

mvn fabric8:deploy -Popenshift -f theatre/pom.xml
mvn fabric8:deploy -Popenshift -f taxi/pom.xml

# lookup the external http endpoint for the services
oc get svc
oc get routes
oc describe route/...

# create theatre and taxi bookings
curl -X POST http://stm-vertx-trip-demo-stmdemo.192.168.99.100.nip.io/api/theatre/Apollo
curl -X POST http://stm-vertx-trip-demo-stmdemo.192.168.99.100.nip.io/api/taxi/ABC

# sharing state between pods/verticles
oc volume dc/stm-vertx --add -t persistentVolumeClaim --claim-name stm-vertx-logs -m /vertx/data




