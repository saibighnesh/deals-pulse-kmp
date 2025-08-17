#!/usr/bin/env sh

APP_BASE_DIR=$(cd "$(dirname "$0")" && pwd -P)
CLASSPATH="$APP_BASE_DIR/gradle/wrapper/gradle-wrapper.jar"

exec java -Dorg.gradle.appname=gradlew -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"