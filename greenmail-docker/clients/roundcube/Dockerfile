# Based on https://github.com/konstantinj/docker-roundcube
FROM alpine:3.5
MAINTAINER https://github.com/greenmail-mail-test/greenmail (marcel.may.de@gmail.com)

ENV VERSION=1.1.7

RUN apk add --no-cache bash curl \
                       php5-common php5-iconv php5-imap php5-xml php5-json php5-dom php5-mcrypt php5-intl php5-zip \
                       php5-pear php5-pdo php5-sqlite3 php5-pdo_sqlite \
                       php5-pear-mail_mime php5-pear-net_smtp \
                       php5-fpm \
 && curl --location https://github.com/roundcube/roundcubemail/releases/download/${VERSION}/roundcubemail-${VERSION}.tar.gz | tar xzf - \
 && mv roundcubemail* /www \
 && curl --remote-name http://svn.apache.org/repos/asf/httpd/httpd/trunk/docs/conf/mime.types \
 && mkdir /config

COPY config.php php.ini run.sh /
CMD /run.sh
