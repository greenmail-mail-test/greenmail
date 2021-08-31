# Roundcube Docker image

Docker container for testing [Roundcube](https://github.com/roundcube/roundcubemail) with GreenMail.

## Features - why using this image instead of several others?

- Uses [official Roundcube docker image](https://hub.docker.com/r/roundcube/roundcubemail/)

## Usage

Run `docker-compose up` and access Roundcube in your browser.

|Port|Description|
|----|-----------|
|80| Roundcube Client | 
|3025| GreenMail SMTP | 
|3110| GreenMail POP3 | 
|3143| GreenMail IMAP | 
|3465| GreenMail SMTPS | 
|3993| GreenMail IMAPS | 
|3995| GreenMail POP3S | 
|8080| GreenMail API |

How to run with docker (instead of docker-compose)
--------

1. Create network
```
docker network create greenmail-network
```

2. Start GreenMail container
```
docker run --tty --interactive --name greenmail \
    --env JAVA_OPTS='-Dgreenmail.verbose' \
   -p 3025:3025 -p 3110:3110 \
   -p 3143:3143 -p 3465:3465 \
   -p 3993:3993 -p 3995:3995 \
   -p 8080:8080 \
   --network='greenmail-network' \
   greenmail/standalone:<VERSION>
```

3. Start Roundcube container
```
docker run --tty --interactive --name roundcube \
    --env ROUNDCUBEMAIL_DEFAULT_HOST='greenmail' \
    --env ROUNDCUBEMAIL_DEFAULT_PORT='3143' \
    --env ROUNDCUBEMAIL_SMTP_SERVER='greenmail' \
    --env ROUNDCUBEMAIL_SMTP_PORT='3025' \
    -p 80:80 \
    --network='greenmail-network' \
    roundcube/roundcubemail:latest
```
