# Roundcube Docker image

Docker container for testing [Roundcube](https://github.com/roundcube/roundcubemail) with GreenMail.

## Features - why using this image instead of several others?

- Uses [alpine](https://registry.hub.docker.com/_/alpine/) base image
- Derived from [konstantinj/docker-roundcube](https://github.com/konstantinj/docker-roundcube)

## Usage

Run `docker-compose up` and access Roundcube in your browser.

|Port|Description|
|----|-----------|
|4080| Roundcube Client| 
|3025| GreenMail SMTP | 
|3143| GreenMail IMAP | 

How to run with docker (instead of docker-compose)
--------

1. Build configured Roundcube image  
   `docker build -t greenmail/client-roundcube .`

2. Start GreenMail
```
docker run -t -i --name greenmail \
           -e GREENMAIL_OPTS='-Dgreenmail.setup.test.all -Dgreenmail.hostname=0.0.0.0 -Dgreenmail.auth.disabled -Dgreenmail.verbose' \
           -p 3025:3025 -p 3143:3143 greenmail/standalon:<VERSION>`
```

3. Start configured Roundcube image

`docker run -t -i --link greenmail -p 4080:80 greenmail/client-roundcube`

