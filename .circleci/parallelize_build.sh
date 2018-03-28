#! /bin/bash

export ADDITONAL_MVN_OPTS=""

copy_artifacts() {
 ARTIFACT_VARIANT_DIR="$CIRCLE_ARTIFACTS/greenmail-$1"
 echo $ARTIFACT_VARIANT_DIR
 mkdir $ARTIFACT_VARIANT_DIR && \
 find . -name \*.log -exec cp -r --parent {} $ARTIFACT_VARIANT_DIR/ \;
}

run_maven() {
  mvn -V $ADDITONAL_MVN_OPTS clean install -DskipTests
  mvn $ADDITONAL_MVN_OPTS test
  return_code=$?
  if [[ $return_code -ne 0 ]] ; then
    echo 'Test failure(s) found. Exiting'; exit $return_code
  fi
}

jdk7() {
  sudo update-alternatives --set java "/usr/lib/jvm/jdk1.7.0/bin/java"
  sudo update-alternatives --set javac "/usr/lib/jvm/jdk1.7.0/bin/javac"
  echo 'export JAVA_HOME=/usr/lib/jvm/jdk1.7.0/' >> ~/.circlerc
  export ADDITONAL_MVN_OPTS="-pl '!greenmail-junit5'"
}

jdk8() {
  sudo update-alternatives --set java "/usr/lib/jvm/jdk1.8.0/bin/java"
  sudo update-alternatives --set javac "/usr/lib/jvm/jdk1.8.0/bin/javac"
  echo 'export JAVA_HOME=/usr/lib/jvm/jdk1.8.0' >> ~/.circlerc
}

openjdk7() {
  sudo update-alternatives --set java "/usr/lib/jvm/java-7-openjdk-amd64/jre/bin/java"
  sudo update-alternatives --set javac "/usr/lib/jvm/java-7-openjdk-amd64/bin/javac"
  echo 'export JAVA_HOME=/usr/lib/jvm/java-7-openjdk-amd64' >> ~/.circlerc
  export ADDITONAL_MVN_OPTS="-pl '!greenmail-junit5'"
}

openjdk8() {
  sudo update-alternatives --set java "/usr/lib/jvm/java-8-openjdk-amd64/jre/bin/java"
  sudo update-alternatives --set javac "/usr/lib/jvm/java-8-openjdk-amd64/bin/javac"
  echo 'export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64' >> ~/.circlerc
}

case $CIRCLE_NODE_INDEX in 
  0)
    echo "Building GreenMail on Oracle JDK7"
    jdk7
    run_maven
    copy_artifacts "jdk1.7.0"
    ;;
  1)
    echo "Building GreenMail on Oracle JDK8"
    jdk8
    run_maven
    copy_artifacts "jdk1.8.0"
    ;;
  2)
    echo "Building GreenMail on OpenJDK7"
    openjdk7
    run_maven
    copy_artifacts "java-7-openjdk-amd64"
    ;;
  3)
    echo "Building GreenMail on OpenJDK8"
    openjdk8
    run_maven
    copy_artifacts "java-8-openjdk-amd64"
    ;;
esac

