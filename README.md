# vertx-sparkpi
A Java implementation of SparkPi using Vert.x 3 

This application is an example tutorial for the
[radanalytics.io](http://radanalytics.io) community. It is intended to be
used as a source-to-image (s2i) application.

## Quick start

You should have access to an OpenShift cluster and be logged in with the
`oc` command line tool.

1. Create the necessary infrastructure objects
   ```bash
   oc create -f http://radanalytics.io/resources.yaml
   ```

1. Launch vertx-sparkpi
   ```bash
   oc new-app --template oshinko-java-spark-build-dc \
       -p APPLICATION_NAME=vertx-sparkpi \
       -p GIT_URI=https://github.com/radanalyticsio/tutorial-sparkpi-java-vertx \
       -p APP_FILE=sparkpi-app-1.0-SNAPSHOT-vertx.jar \
       -p SPARK_OPTIONS='--driver-java-options="-Dvertx.cacheDirBase=/tmp/vertx-cache"'
   ```

1. Expose an external route
   ```bash
   oc expose svc/vertx-sparkpi
   ```

1. Visit the exposed URL with your browser or other HTTP tool, for example:
   ```bash
   $ curl http://`oc get routes/vertx-sparkpi --template='{{.spec.host}}'`
   Pi is rouuuughly 3.1335
   ```
