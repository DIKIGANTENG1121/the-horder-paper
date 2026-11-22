#!/usr/bin/env bash
set -euo pipefail

# Mode A / Local Runner for Paper 26.3 (Java 25) + HordesPaper + NeonTestKit
echo "=== 1. Verifying Java 25 ==="
java -version

echo "=== 2. Building HordesPaper and NeonTestKit ==="
gradle build :neon-testkit:build

mkdir -p run-test/plugins
cp build/libs/HordesPaper-*.jar run-test/plugins/
cp neon-testkit/build/libs/neon-testkit-*.jar run-test/plugins/

echo "eula=true" > run-test/eula.txt
if [ ! -f run-test/server.properties ]; then
  printf "online-mode=false
level-name=world
" > run-test/server.properties
fi

echo "=== 3. Place Paper 26.3 server jar at run-test/paper-26.3.jar and run: ==="
echo "cd run-test && java -Xms2G -Xmx4G -jar paper-26.3.jar --nogui"
echo "Then execute in console: neontest run ALL"
