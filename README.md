# GreenMail
[![Build status](https://circleci.com/gh/greenmail-mail-test/greenmail/tree/master.svg?style=shield)](https://app.circleci.com/pipelines/github/greenmail-mail-test) [![Maven Central](https://img.shields.io/maven-central/v/com.icegreen/greenmail.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.icegreen%22%20AND%20a%3A%22greenmail%22) [![StackOverflow](http://img.shields.io/badge/stackoverflow-greenmail-green.svg)](http://stackoverflow.com/questions/tagged/greenmail) [![Docker Pulls](https://img.shields.io/docker/pulls/greenmail/standalone.svg?maxAge=604800)][docker-hub] [![javadoc](https://javadoc.io/badge2/com.icegreen/greenmail/javadoc.svg)](https://javadoc.io/doc/com.icegreen/greenmail)

[GreenMail][greenmail_project_site] allows developers to test email-based applications, services or systems without access to a live mail server.
Developers can send, receive, and verify emails by embedding GreenMail in a unit test or running it as a standalone container.
GreenMail acts as a virtual (mocking/sandbox) mail server and supports common mail protocols SMTP, IMAP and POP3.

* [Examples][greenmail_examples]
* [Javadoc][greenmail_javadoc]
* [FAQ][greenmail_faq]
* [Download][greenmail_download]
* [Maven coordinates][maven_repository_com]: com.icegreen:greenmail:\<[VERSION](https://github.com/greenmail-mail-test/greenmail/releases/)\>

The separate [GreenMail Client Integrations project](https://github.com/greenmail-mail-test/greenmail-client-integrations) provides
containerized example integration of GreenMail with various web mail clients. 

The GreenMail project welcomes any contribution, so go ahead and fork/open a pull request! See the guidelines below.

## Version compatibility

| GreenMail | Mail API                   | Example frameworks                                                   |
|-----------|----------------------------|----------------------------------------------------------------------|
| 2.1.x     | [JakartaMail 2.1.x][jm_21] | [Jakarta EE 10][jakarta_ee_10]                                       |
| 2.0.x     | [JakartaMail 2.0.x][jm_20] | [Jakarta EE 9][jakarta_ee_9], Spring 6,                              |
| 1.6.x     | [JakartaMail 1.6.x][jm_16] | [Jakarta EE 8][jakarta_ee_8], Spring 5, Apache commons-mail 1.5, ... |

## Development

* Build GreenMail from source 

  `mvn clean install -Pdocker`

  Make sure you got [Maven 3.9][maven_download] or higher, and run a JDK 11 or newer.
  If you want to skip building the docker image, leave out the `-Pdocker` profile option.

  If you want to skip the long-running tests, use the Maven option `-DskipITs` .

* Build the Maven site (and the optional example report)

  `mvn site -Psite`

* Build and deploy a release

  For rolling a release including version increment and release upload, do

  `mvn clean release:prepare -Prelease,release-ossrh,docker,docker-tag-latest`
  `mvn release:perform -Prelease,release-ossrh,docker,docker-tag-latest`

  For a tagged release and deployment to [Sonatype OpenSource Repository Hosting][ossrh_maven] and later syncing to [Maven Central][maven_repository_release], do

  `mvn clean deploy -Prelease,release-ossrh,docker,docker-tag-latest`

  Note: Do only use docker-tag-latest profile if you really want the tag latest, e.g. for newest release of the highest version.

* Build and deploy a snapshot

  For a Maven Snapshot deployment to [Sonatype][maven_repository_snapshot], do

  `mvn clean deploy -Prelease-ossrh,docker`

* Check [Sonar][sonar] report

[greenmail_project_site]: https://greenmail-mail-test.github.io/greenmail/
[greenmail_examples]: https://greenmail-mail-test.github.io/greenmail#examples
[greenmail_faq]: https://greenmail-mail-test.github.io/greenmail#faq
[greenmail_javadoc]: https://javadoc.io/doc/com.icegreen/greenmail
[greenmail_download]: https://greenmail-mail-test.github.io/greenmail#download
[greenmail_sf_site]: https://sourceforge.net/p/greenmail
[maven_repository_com]: http://mvnrepository.com/artifact/com.icegreen/greenmail
[maven_download]: http://maven.apache.org
[ossrh_maven]: http://central.sonatype.org/pages/apache-maven.html
[maven_repository_snapshot]: https://oss.sonatype.org/content/repositories/snapshots/com/icegreen/
[maven_repository_release]: http://central.maven.org/maven2/com/icegreen/
[github_fork]: https://help.github.com/articles/fork-a-repo/
[github_pull_request]: https://help.github.com/articles/creating-a-pull-request/
[sonar]: http://nemo.sonarqube.org/dashboard/index?id=com.icegreen%3Agreenmail-parent
[docker-hub]: https://hub.docker.com/r/greenmail/standalone/
[jm_21]: https://projects.eclipse.org/projects/ee4j.mail
[jm_20]: https://projects.eclipse.org/projects/ee4j.mail
[jm_16]: https://projects.eclipse.org/projects/ee4j.mail
[jakarta_ee_10]: https://jakarta.ee/release/10/
[jakarta_ee_9]: https://jakarta.ee/release/9/
[jakarta_ee_8]: https://jakarta.ee/release/8/

## Roadmap

* [2.1](https://github.com/greenmail-mail-test/greenmail/milestone/39)
  * Baseline: Jakarta EE 10
    * JakartaMail 2.1 / Angus Mail
    * Java 11
    * Jersey 3.1.x
  * Only junit 5?
* [2.0](https://github.com/greenmail-mail-test/greenmail/milestone/3) ([branch master](https://github.com/greenmail-mail-test/greenmail/tree/master))
  * Baseline: Jakarta EE 9
    * JakartaMail 2.0
    * Jersey 3.0.x for servlet 5 / restfulWS-3.0
    * Java 11 for building / running integration tests
  * Java 8
  * Deprecations (no GreenMailRule in greenmail-core, ...)
* [1.6](https://github.com/greenmail-mail-test/greenmail/issues?q=is%3Aopen+is%3Aissue+milestone%3A1.6) ([branch releases/1.6.x](https://github.com/greenmail-mail-test/greenmail/tree/releases/1.6.x))
  * Bugfix and maintenance

## Contribution guidelines

We really appreciate your contribution!
To make it easier for integrating your contribution, have a look at the following guidelines.

### Be concise

Try to keep your changes focused. Please avoid (major) refactorings and avoid re-formatting existing code.
A good check is looking at the diff of your pull request.
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
The best strategy for opening a [pull request][github_pull_request] after a [fork][github_fork] is to add this [repository](https://github.com/greenmail-mail-test/greenmail)
as the "upstream" to your .git/config such as:

    [remote "upstream"]
    url = https://github.com/greenmail-mail-test/greenmail.git
    fetch = +refs/heads/*:refs/remotes/upstream/*

Then you fetch "upstream" and create a new branch at upstream/master (name it issue-XXX or something like that).
Now you can add commits on that branch and then create a pull request for that branch (after pushing it to your
GitHub). That way commits are isolated for one feature.

### Tests for your pull request ###
Please also create a test for every feature you add. We know that currently there aren't many tests but in
the medium term we want to increase test coverage.

Misc
----
Many thanks to [JProfiler](http://www.ej-technologies.com/products/jprofiler/overview.html) and [Jetbrains](https://www.jetbrains.com/) for supporting this project with free OSS licenses

