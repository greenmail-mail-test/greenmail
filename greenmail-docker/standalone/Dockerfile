FROM openjdk:7-jdk-alpine
MAINTAINER https://github.com/greenmail-mail-test/greenmail (marcel.may.de@gmail.com)

# Expose ports:
# - smtp  : 3025
# - smtps : 3465
# - pop3  : 3110
# - pop3s : 3995
# - imap  : 3143
# - imaps : 3993
EXPOSE 3025 3465 3110 3995 3143 3993

# Run as user greenmail
RUN addgroup greenmail && adduser -G greenmail -D greenmail
# Note: If using Dockerfile without Maven, you must manually copy the JAR
ADD target/greenmail-standalone.jar /home/greenmail/greenmail-standalone.jar
RUN chown greenmail:greenmail /home/greenmail/greenmail-standalone.jar
USER greenmail
WORKDIR /home/greenmail
# Run GreenMail Standalone with test setup and disabled authentication
ENTRYPOINT ["java", \
    "-Djava.net.preferIPv4Stack=true", \
    "-Dgreenmail.setup.test.all", \
    "-Dgreenmail.hostname=0.0.0.0", \
    "-Dgreenmail.auth.disabled", \
    "-jar", "greenmail-standalone.jar"]
