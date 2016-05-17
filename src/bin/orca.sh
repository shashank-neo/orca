#!/usr/bin/env bash
HOST="127.0.0.1"
PORT=9090
while [[ $# > 1 ]]
do
key="$1"
case $key in
    -mo|--mode)
    MODE="$2"
    shift # past argument
    ;;
    -ho|--host)
    HOST="$2"
    shift # past argument
    ;;
esac
shift # past argument or value
done

java -server -Dcom.sun.management.jmxremote -Djava.rmi.server.hostname -Dcom.sun.management.jmxremote.port=${PORT} -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Xmn2048M -Xms4096M -Xmx4096M -XX:PermSize=128M -XX:MaxPermSize=128M -XX:ReservedCodeCacheSize=64m -Xss256k -XX:+UseConcMarkSweepGC -XX:ParallelGCThreads=4 -XX:+UseParNewGC -XX:SurvivorRatio=8 -XX:CompileThreshold=100 -cp .:./lib/*:orca-1.0.jar com.falcon.orca.Main -mo "${MODE}" -ho "${HOST}"
