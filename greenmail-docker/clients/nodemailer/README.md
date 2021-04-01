GreenMail test setup with nodemailer
=========

This is a POC showing a GreenMail and [nodemailer](https://github.com/nodemailer/nodemailer) setup, configuring nodemailer to send a single message to GreenMail.

Run `docker-compose up --build` and use eg Thunderbird to verify

|Port|Description|
|----|-----------|
|3025| GreenMail SMTP | 
|3143| GreenMail IMAP | 
|8080| GreenMail API  | 
