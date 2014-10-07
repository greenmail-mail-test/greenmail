GreenMail
=========

[GreenMail][greenmail_project_site] is an open source, intuitive and easy-to-use test suite of email servers for testing purposes. 
Supports SMTP, POP3, IMAP with SSL socket support. GreenMail also provides a JBoss GreenMail Service.
GreenMail is the fist and only library that offers a test framework for both receiving and retrieving emails from Java.

Go to the [project site][greenmail_project_site] for details:

* [Examples][greenmail_examples]
* [JavaDoc][greenmail_javadoc]
* [FAQ][greenmail_faq]
* [Download][greenmail_download]
* [Maven coordinates][maven_repository_com]: com.icegreen:greenmail:1.3.1b

The GreenMail project welcomes any contribution, so go ahead and fork/open a pull request!

***Note***: GreenMail recently moved to Github and was previously hosted on [SF][greenmail_sf_site].

Development
-----------

* Build GreenMail from source 

    mvn clean install

  Make sure you got [Maven 3.0.5+][maven_download] or higher.

* Build the site (and the optional example report)

    mvn site -Psite

* Build a release

    mvn clean install -Prelease 

[greenmail_project_site]: http://www.icegreen.com/greenmail
[greenmail_examples]: http://www.icegreen.com/greenmail/examples.html
[greenmail_faq]: http://www.icegreen.com/greenmail/faq.html
[greenmail_javadoc]: http://www.icegreen.com/greenmail/javadocs/index.html
[greenmail_download]: http://www.icegreen.com/greenmail/download.html
[greenmail_sf_site]: https://sourceforge.net/p/greenmail
[maven_repository_com]: http://mvnrepository.com/artifact/com.icegreen/greenmail
[maven_download]: http://maven.apache.org
