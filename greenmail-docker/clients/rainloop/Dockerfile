FROM solidnerd/rainloop:1.10.5.192
MAINTAINER https://github.com/greenmail-mail-test/greenmail (marcel.may.de@gmail.com)

EXPOSE 80

# Replace domains with preconfigured GreenMail domain
RUN rm $RAINLOOP_HOME/rainloop/v/1.10.5.192/app/domains/*
ADD domains/* $RAINLOOP_HOME/rainloop/v/1.10.5.192/app/domains/
