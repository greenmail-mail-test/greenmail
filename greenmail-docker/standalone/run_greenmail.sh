#/bin/sh
echo "Executing 'java $JAVA_OPTS $GREENMAIL_OPTS -jar greenmail-standalone.jar' ..."
exec java $JAVA_OPTS $GREENMAIL_OPTS -jar greenmail-standalone.jar
