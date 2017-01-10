GreenMail test setup with Rainloop
=========

Extends [solidnerd/rainloop:1.10.5.192](https://github.com/solidnerd/docker-rainloop) Docker image with a configured GreenMail domain.

Run `docker-compose up .` and access Rainloop in your browser.

|Port|Description|
|----|-----------|
|3080| Rainloop  | 
|3025| GreenMail SMTP | 
|3143| GreenMail IMAP | 

How to run with docker (instead of docker-compose)
--------

1. Build configured Rainloop image  
   `docker build -t greenmail/client-rainloop .`

2. Start GreenMail
```
docker run -t -i --name greenmail \
           -e GREENMAIL_OPTS='-Dgreenmail.setup.test.all -Dgreenmail.hostname=0.0.0.0 -Dgreenmail.auth.disabled -Dgreenmail.verbose' \
           -p 3025:3025 -p 3143:3143 greenmail/standalon:<VERSION>`
```

3. Start configured Rainloop image

`docker run -t -i --link greenmail -p 3080:80 greenmail/client-rainloop`

How to run with docker-compose
--------

You can - as a As an alternative to starting each container individually you can use docker-compose.
Check and modify the greenmail image version in [docker-compose.yml] and run `docker-compose up`.
