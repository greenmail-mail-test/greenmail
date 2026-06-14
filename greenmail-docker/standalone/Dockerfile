FROM azul/zulu-openjdk:11.0.31-jre

LABEL org.opencontainers.image.url=https://github.com/greenmail-mail-test/greenmail/blob/master/greenmail-docker/standalone
LABEL org.opencontainers.image.source=https://github.com/greenmail-mail-test/greenmail/blob/master/greenmail-docker/standalone/Dockerfile

#######################
# Expose ports:
# - smtp  : 3025
# - smtps : 3465
# - pop3  : 3110
# - pop3s : 3995
# - imap  : 3143
# - imaps : 3993
# - api   : 8080
#######################
EXPOSE 3025 3465 3110 3995 3143 3993 8080

#######################
# Configuration options
#######################
ENV JAVA_OPTS="-Djava.net.preferIPv4Stack=true"
# Default GreenMail configuration options
ENV GREENMAIL_OPTS="-Dgreenmail.setup.test.all \
    -Dgreenmail.hostname=0.0.0.0 \
    -Dgreenmail.tls.keystore.file=/home/greenmail/greenmail.p12 \
    -Dgreenmail.tls.keystore.password=changeit \
    -Dgreenmail.auth.disabled"
# Provides additional GreenMail options. Useful for extending default GREENMAIL_OPTS.
ENV GREENMAIL_ADDITIONAL_OPTS=""

#######################
# Run as user greenmail
RUN groupadd -r greenmail && useradd --no-log-init -r -g greenmail greenmail
# Note: If using Dockerfile without Maven, you must manually copy the JAR
COPY --chown=greenmail:greenmail target/greenmail-standalone.jar /home/greenmail/greenmail-standalone.jar
COPY --chown=greenmail:greenmail target/greenmail.p12 /home/greenmail/greenmail.p12
COPY --chown=greenmail:greenmail run_greenmail.sh /home/greenmail/run_greenmail.sh
RUN chmod +x /home/greenmail/run_greenmail.sh
USER greenmail
WORKDIR /home/greenmail
# Run GreenMail Standalone with test setup and disabled authentication
ENTRYPOINT ["/home/greenmail/run_greenmail.sh"]
