#!/bin/bash

DIR="target/hola-swarm.jar.dir"
YAML="$DIR/META-INF/wildfly-swarm-manifest.yaml"

function writeto() {
  echo "$1" >> $YAML
}

set -x -e
rm -rf "$DIR"
unzip -q -d "$DIR" target/hola-swarm.jar
VERSION_CLIENT=`find target/hola-swarm.jar.dir -name 'httpclient*jar' | head -n 1 | sed 's|.*-\(.*\)\.jar$|\1|'`
VERSION_CORE=`find target/hola-swarm.jar.dir -name 'httpcore*jar' | head -n 1 | sed 's|.*-\(.*\)\.jar$|\1|'`

if [ "x$1" = "xnew" ]; then
  writeto "  org.apache.httpcomponents:httpclient:jar:${VERSION_CLIENT}: null"
  writeto "  org.apache.httpcomponents:httpcore:jar:${VERSION_CORE}: null"
else
  writeto "- org.apache.httpcomponents:httpclient:jar:${VERSION_CLIENT}"
fi
sed -i '/org.jboss.resteasy:/d' "$YAML"

[ ! -f hola-swarm.jar.origin ] && mv target/hola-swarm.jar target/hola-swarm.jar.origin

cd target/hola-swarm.jar.dir
zip -q -r ../hola-swarm.jar .
cd -

