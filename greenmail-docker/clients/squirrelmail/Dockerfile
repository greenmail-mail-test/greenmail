FROM ubuntu:16.10
MAINTAINER marcel.may.de@gmail.com

# Squirrelmail
RUN apt-get update && apt-get install -y squirrelmail vim screen php-imap php-common apache2-doc apache2-suexec-custom apache2-utils php-pear php-recode php-ldap php-net-imap php-dev subversion g++ gawk autoconf automake libtool bison php-sqlite3 sqlite3 libsqlite3-dev libreadline6-dev zlib1g-dev libssl-dev libyaml-dev libgdbm-dev libncurses5-dev libffi-dev python-pip pylint libc6-i386 lib32z1

#RUN ln -s /etc/squirrelmail/apache.conf /etc/apache2/conf.d/squirrelmail.conf
RUN ln -s /etc/squirrelmail/apache.conf /etc/apache2/sites-enabled/squirrelmail.conf
COPY config.php /etc/squirrelmail/config.php
EXPOSE 80
CMD apachectl start && tail -f /var/log/apache2/error.log
