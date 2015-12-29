GreenMail Standalone Docker Image
=========

Builds a docker image running [GreenMail Standalone](http://www.icegreen.com/greenmail/index.html#deploy_standalone).
For a detailed description, see [GreenMail Docker documentation](http://www.icegreen.com/greenmail/index.html#deploy_docker_standalone). The prebuilt docker image is also available via [Docker Hub](https://hub.docker.com/r/greenmail/standalone/).

By default, the image runs GreenMail using the test setup ports (default mail ports plus an offset of 3000).

How to build via docker CLI
------------

1. Build image  
   `docker build -t greenmail/standalone .`

2. Test  
   `docker run -t -i -p 3025:3025 -p 3110:3110 -p 3143:3143 -p 3465:3465 -p 3993:3993 -p 3995:3995  greenmail/standalone`

3. Deploy to [Docker Hub](https://hub.docker.com/r/greenmail/standalone/)  
   `docker push greenmail/standalone`

How to build via Maven
----------------------

Maven uses the excellent [maven docker plugin](https://github.com/rhuss/docker-maven-plugin/).

A quickstart when using Maven from this module:

1. Building  
   `mvn clean docker:build`

2. Running  
   `mvn docker:start`

3. Stopping  
   `mvn docker:stop`

4. Pushing to Docker Hub  
   `mvn docker:push`

Note: For running from GreenMail top level Maven root, use the docker profile which is not active by default:  
`mvn clean install -Pdocker`
