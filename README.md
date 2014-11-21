GreenMail
=========

[![build status](https://api.travis-ci.org/greenmail-mail-test/greenmail.svg?branch=master)](https://travis-ci.org/greenmail-mail-test/greenmail)

[GreenMail][greenmail_project_site] is an open source, intuitive and easy-to-use test suite of email servers for testing purposes. 
Supports SMTP, POP3, IMAP with SSL socket support. GreenMail also provides a JBoss GreenMail Service.
GreenMail is the fist and only library that offers a test framework for both receiving and retrieving emails from Java.

Go to the [project site][greenmail_project_site] for details:

* [Examples][greenmail_examples]
* [JavaDoc][greenmail_javadoc]
* [FAQ][greenmail_faq]
* [Download][greenmail_download]
* [Maven coordinates][maven_repository_com]: com.icegreen:greenmail:1.4.0

The GreenMail project welcomes any contribution, so go ahead and fork/open a pull request! See the guidelines below.

***Note***: GreenMail recently moved to Github and was previously hosted on [SF][greenmail_sf_site].

Development  [![Build Status](https://travis-ci.org/greenmail-mail-test/greenmail.svg?branch=master)](https://travis-ci.org/greenmail-mail-test/greenmail)
-----------

* Build GreenMail from source 

  `mvn clean install`

  Make sure you got [Maven 3.0.5+][maven_download] or higher.

* Build the Maven site (and the optional example report)

  `mvn site -Psite`

  To build the project site using [jekyll][jekyll], run

  `cd greenmail-site`

  `jekyll build`

* Build and deploy a release

  For a tagged release and deployment to [Sonatype OpenSource Repository Hosting][ossrh_maven], do

  `mvn clean deploy -Prelease,release-ossrh`

[greenmail_project_site]: http://www.icegreen.com/greenmail
[greenmail_examples]: http://www.icegreen.com/greenmail/examples.html
[greenmail_faq]: http://www.icegreen.com/greenmail/faq.html
[greenmail_javadoc]: http://www.icegreen.com/greenmail/javadocs/index.html
[greenmail_download]: http://www.icegreen.com/greenmail/download.html
[greenmail_sf_site]: https://sourceforge.net/p/greenmail
[maven_repository_com]: http://mvnrepository.com/artifact/com.icegreen/greenmail
[maven_download]: http://maven.apache.org
[jekyll]: http://jekyllrb.com/
[ossrh_maven]: http://central.sonatype.org/pages/apache-maven.html

Contribution guidelines
-----------------------

### Code formatter ###
Please set your code formatter to use 4 spaces for indentation of Java files (not tabs) and
to two spaces for xml files (like the pom.xml)

### Bill of Materials ###
We have the pom.xml in the root where we set the versions of all dependencies to keep them consistent
among subprojects. Please do not add any version tags into the child pom.xml files.

### Starting your pull request ###
The best strategy for opening a pull request is to add the this repository ( https://github.com/greenmail-mail-test/greenmail )
as the "upstream" to your .git/config such as:

    [remote "upstream"]
    url = https://github.com/greenmail-mail-test/greenmail.git
    fetch = +refs/heads/:refs/remotes/upstream/

Then you fetch "upstream" and create a new branch at upstream/master (name it issue-XXX or something like that).
Now you can add commits on that branch and then create a pull request for that branch (after pushing it to your
github). That way commits are isolated for one feature.

### Tests for your pull request ###
Please also create a test for every feature you add. We know that currently there aren't many tests but in
the medium term we want to increase test coverage.
