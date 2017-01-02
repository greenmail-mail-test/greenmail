#/bin/sh
java \
    -Djava.net.preferIPv4Stack=true \
    -Dgreenmail.setup.test.all \
    -Dgreenmail.hostname=0.0.0.0 \
    -Dgreenmail.auth.disabled \
    -jar greenmail-standalone.jar
