<?xml version="1.0" encoding="UTF-8"?>

<project xmlns="http://maven.apache.org/POM/4.0.0"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>tools.vitruv</groupId>
    <artifactId>${packageName}</artifactId>
    <version>3.2.0-SNAPSHOT</version>
  </parent>

  <artifactId>${packageName}.p2wrappers</artifactId>
  <packaging>pom</packaging>

  <name>p2 Dependency Wrappers</name>
  <description />

  <modules>
    <module>activextendannotations</module>
    <module>emfutils</module>
    <module>javautils</module>
  </modules>

  <build>
    <pluginManagement>
      <plugins>
        <!-- allow Eclipse updatesites as repositories -->
        <plugin>
          <groupId>org.openntf.maven</groupId>
          <artifactId>p2-layout-resolver</artifactId>
          <version>1.9.0</version>
          <extensions>true</extensions>
        </plugin>
        <!-- generate empty javadoc JARs for deployment -->
        <plugin>
          <artifactId>maven-jar-plugin</artifactId>
          <version>3.4.2</version>
          <executions>
            <execution>
              <id>javadoc-jar</id>
              <phase>package</phase>
              <goals>
                <goal>jar</goal>
              </goals>
              <configuration>
                <classifier>javadoc</classifier>
              </configuration>
            </execution>
          </executions>
        </plugin>
      </plugins>
    </pluginManagement>
  </build>

  <pluginRepositories>
    <!-- required for the p2-layout-resolver plugin -->
    <pluginRepository>
      <id>artifactory.openntf.org</id>
      <name>artifactory.openntf.org</name>
      <url>https://artifactory.openntf.org/openntf</url>
      <snapshots>
        <enabled>false</enabled>
      </snapshots>
    </pluginRepository>
  </pluginRepositories>
</project>