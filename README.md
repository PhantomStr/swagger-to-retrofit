# SWAGGER TO RETROFIT

The module provide generation of retrofit services and models.

## Getting Started

run mvn compile if prepared phase compile<br>
plugin can be executed directly: mvn swagger-to-retrofit:generate <br>

### setup

need prepare pom with parameter io.github.phantomstr.testing.tool.swagger2retrofit.swagger2retrofit.commandline<br>
where <br>
*required*<br>
-u or --url : path to swagger api <br>
*optional*<br>
-mp or --modelsPackage : package where will be compiled models<br>
-sp or --servicePackage : package where will be compiled services<br>
-ar or --apiRoot : root of api. Will be removed from path start<br>
-sf or --serviceFilters : regexp filter of generated services<br>
**example:**

```xml

<project>
  <properties>
    <io.github.phantomstr.testing.tool.swagger2retrofit.swagger2retrofit.commandline>-u http://localhost:8080/v2/api-docs -mp io.github.phantomstr.testing.tool.rest.model -sp
      io.github.phantomstr.testing.tool.rest.service
    </io.github.phantomstr.testing.tool.swagger2retrofit.swagger2retrofit.commandline>
  </properties>
  <build>
    <plugins>
      <plugin>
        <groupId>io.github.phantomstr.testing-tools</groupId>
        <artifactId>swagger-to-retrofit</artifactId>
        <version>1.1.2</version>
        <executions>
          <execution>
            <phase>validate</phase>
            <goals>
              <goal>generate</goal>
            </goals>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
</project>
```

### Prerequisites

- For scheme generation lombok 1.18.20 in local repository is required.

- Lombok plugin required File | Settings | Plugins | Marketplace | find "lombok" | install

- Annotation processing required (check option): <br>
  File | Settings | Build, Execution, Deployment | Compiler | Annotation Processors |Enable annotation processing

## Built With

* [Maven](https://maven.apache.org/) - Dependency Management

## Versioning

We use [SemVer](http://semver.org/) for versioning.