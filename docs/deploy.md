# How to deploy to the maven central repository
This document describes the process of deploying a new artifact to the maven central repository.

## Prerequisites
- Java JDK 11 or later
- Maven 3.8.3 or later
- GPG installed and configured ask Maks about 
- Sonatype account [Sonatype publishing page](https://central.sonatype.com/publishing) ask Maks about

## Deployment
```shell
mvn clean deploy
```

# References:
1. [Our libraries](https://repo1.maven.org/maven2/ltd/clear-solutions/)
2. [Register to Publish Via the Central Portal](https://central.sonatype.org/register/central-portal/)
3. [Publish to the maven](https://central-stage.sonatype.org/publish/publish-portal-maven/)