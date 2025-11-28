<?xml version="1.0" encoding="UTF-8"?>

<project xmlns="http://maven.apache.org/POM/4.0.0"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>tools.vitruv</groupId>
    <artifactId>${packageName}.p2wrappers</artifactId>
    <version>0.1.0-SNAPSHOT</version>
  </parent>

  <artifactId>${packageName}.p2wrappers.activextendannotations</artifactId>

  <name>p2 Dependency Wrapper Active Xtend Annotations</name>
  <description>wrapper for the p2 dependency xannotations:edu.kit.ipd.sdq.activextendannotations</description>
  <#noparse>
  <build>
    <plugins>
      <plugin>
        <groupId>org.openntf.maven</groupId>
        <artifactId>p2-layout-resolver</artifactId>
      </plugin>
      <plugin>
        <groupId>org.codehaus.mojo</groupId>
        <artifactId>flatten-maven-plugin</artifactId>
      </plugin>
      <plugin>
        <artifactId>maven-shade-plugin</artifactId>
      </plugin>
      <plugin>
        <artifactId>maven-jar-plugin</artifactId>
      </plugin>
    </plugins>
  </build>

  <dependencies>
<dependency>
    <groupId>tools.vitruv</groupId>
    <artifactId>tools.vitruv.dsls.p2wrappers.activextendannotations</artifactId>
    <version>3.2.3</version>
</dependency>
  </dependencies>
</project>
</noparse>