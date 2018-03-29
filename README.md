GreenMail
=========

[GreenMail][greenmail_project_site] is an open source, intuitive and easy-to-use test suite of email servers for testing purposes. 
Supports SMTP, POP3, IMAP with SSL socket support. GreenMail also provides a JBoss GreenMail Service.
GreenMail is the first and only library that offers a test framework for both receiving and retrieving emails from Java.

Go to the [project site][greenmail_project_site] for details:

* [Examples][greenmail_examples]
* [JavaDoc][greenmail_javadoc]
* [FAQ][greenmail_faq]
* [Download][greenmail_download]
* [Maven coordinates][maven_repository_com]: com.icegreen:greenmail:1.5.7

The GreenMail project welcomes any contribution, so go ahead and fork/open a pull request! See the guidelines below.

***Note***: GreenMail recently moved to Github and was previously hosted on [SF][greenmail_sf_site].

Development [![Build status](https://circleci.com/gh/greenmail-mail-test/greenmail/tree/master.svg?style=shield)](https://circleci.com/gh/greenmail-mail-test/greenmail/tree/master) [![Maven Central](https://img.shields.io/maven-central/v/com.icegreen/greenmail.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.icegreen%22%20AND%20a%3A%22greenmail%22)
-----------

* Build GreenMail from source 

  `mvn clean install -Pdocker`

  Make sure you got [Maven 3.2.1+][maven_download] or higher.
  If you want to skip building the docker image, leave out the `-Pdocker` profile option.

  If you want to skip the long running tests, use the Maven option `-DskipITs` .

* Build the Maven site (and the optional example report)

  `mvn site -Psite`

* Build and deploy a release

  For rolling a release including version increment and release upload, do

  `mvn clean release:prepare -Prelease,release-ossrh,docker,docker-tag-latest`
  `mvn release:perform -Prelease,release-ossrh,docker,docker-tag-latest`

  For a tagged release and deployment to [Sonatype OpenSource Repository Hosting][ossrh_maven] and later syncing to [Maven Central][maven_repository_release], do

  `mvn clean deploy -Prelease,release-ossrh,docker,docker-tag-latest`

  Note: Do only use docker-tag-latest profile if you really want the tag latest, e.g. for newest release of highest version.

* Build and deploy a snapshot

  For a Maven Snapshot deployment to [Sonatype][maven_repository_snapshot], do

  `mvn clean deploy -Prelease-ossrh,docker`

* Check [Sonar][sonar] report

[greenmail_project_site]: http://www.icegreen.com/greenmail
[greenmail_examples]: http://www.icegreen.com/greenmail/#examples
[greenmail_faq]: http://www.icegreen.com/greenmail/#faq
[greenmail_javadoc]: http://www.icegreen.com/greenmail/javadocs/index.html
[greenmail_download]: http://www.icegreen.com/greenmail/#download
[greenmail_sf_site]: https://sourceforge.net/p/greenmail
[maven_repository_com]: http://mvnrepository.com/artifact/com.icegreen/greenmail
[maven_download]: http://maven.apache.org
[ossrh_maven]: http://central.sonatype.org/pages/apache-maven.html
[maven_repository_snapshot]: https://oss.sonatype.org/content/repositories/snapshots/com/icegreen
[maven_repository_release]: http://central.maven.org/maven2/com/icegreen/
[github_fork]: https://help.github.com/articles/fork-a-repo/
[github_pull_request]: https://help.github.com/articles/creating-a-pull-request/
[sonar]: http://nemo.sonarqube.org/dashboard/index?id=com.icegreen%3Agreenmail-parent

Contribution guidelines
-----------------------

We really appreciate your contribution!
To make it easier for integrating your contribution, have a look at the following guidelines.

### Be concise

Try to keep your changes focused. Please avoid (major) refactorings and avoid re-formatting existing code.
A good check is looking at the diff of the your pull requrest.
Also, please refer to the open issue you're fixing by including a reference in your commit message.

### Code formatter ###
Please set your code formatter to use 4 spaces for indentation of Java files (not tabs) and
to two spaces for xml files (like the pom.xml). As a general best practise,
your contribution should adhere to existing code style.

### Bill of Materials ###
We have the pom.xml in the root where we set the versions of all dependencies to keep them consistent
among subprojects. Please do not add any version tags into the child pom.xml files.

Please also do not introduce new dependencies as we try to keep these to a minimum.
If you think you require a new dependencies or dependency update,
discuss this up front with committers.

### Starting your pull request ###
The best strategy for opening a [pull request][github_pull_request] after a [fork][github_fork] is to add the this [repository](https://github.com/greenmail-mail-test/greenmail)
as the "upstream" to your .git/config such as:

    [remote "upstream"]
    url = https://github.com/greenmail-mail-test/greenmail.git
    fetch = +refs/heads/*:refs/remotes/upstream/*

Then you fetch "upstream" and create a new branch at upstream/master (name it issue-XXX or something like that).
Now you can add commits on that branch and then create a pull request for that branch (after pushing it to your
github). That way commits are isolated for one feature.

### Tests for your pull request ###
Please also create a test for every feature you add. We know that currently there aren't many tests but in
the medium term we want to increase test coverage.

Misc
----
Many thanks to [JProfiler](http://www.ej-technologies.com/products/jprofiler/overview.html) and [Jetbrains](https://www.jetbrains.com/) for supporting this project with free OSS licenses

