# SquirrelMail Docker image

Docker container for testing [SquirrelMail](https://squirrelmail.org/) with GreenMail.


## Usage

Run `docker-compose up` and access SquirrelMail in your browser.

|Port|Description|
|----|-----------|
|5080| SquirrelMail Client|
|3025| GreenMail SMTP |
|3143| GreenMail IMAP |

How to run with docker (instead of docker-compose)
--------

1. Build configured SquirrelMail image
   `docker build -t greenmail/client-squirrelmail .`

2. Start GreenMail
```
docker run -t -i --name greenmail \
           -e GREENMAIL_OPTS='-Dgreenmail.setup.test.all -Dgreenmail.hostname=0.0.0.0 -Dgreenmail.auth.disabled -Dgreenmail.verbose' \
           -p 3025:3025 -p 3143:3143 greenmail/standalone:<VERSION>`
```

3. Start configured SquirrelMail image

`docker run -t -i --link greenmail -p 5080:80 greenmail/client-squirrelmail`

