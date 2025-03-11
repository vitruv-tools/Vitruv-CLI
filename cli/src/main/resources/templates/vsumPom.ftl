<?xml version="1.0" encoding="UTF-8"?>

<project xmlns="http://maven.apache.org/POM/4.0.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>tools.vitruv</groupId>
        <artifactId>${packageName}</artifactId>
        <version>0.1.0-SNAPSHOT</version>
    </parent>

    <artifactId>${packageName}.vsum</artifactId>

    <name>Vsum</name>
    <description />


    <dependencies>
        <!-- project dependencies -->
        <dependency>
          <#noparse>
            <groupId>${project.groupId}</groupId>
          </#noparse>
            <artifactId>${packageName}.model</artifactId>
              <#noparse>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>${project.groupId}</groupId>
            </#noparse>
            <artifactId>${packageName}.consistency</artifactId>
            <#noparse>
            <version>${project.version}</version>
        </dependency>

        <!-- Vitruvius dependencies -->
        <dependency>
            <groupId>tools.vitruv</groupId>
            <artifactId>tools.vitruv.change.interaction</artifactId>
        </dependency>
        <dependency>
            <groupId>tools.vitruv</groupId>
            <artifactId>tools.vitruv.change.propagation</artifactId>
        </dependency>
        <dependency>
            <groupId>tools.vitruv</groupId>
            <artifactId>tools.vitruv.change.testutils.integration</artifactId>
        </dependency>
        <dependency>
            <groupId>tools.vitruv</groupId>
            <artifactId>tools.vitruv.framework.views</artifactId>
        </dependency>
        <dependency>
            <groupId>tools.vitruv</groupId>
            <artifactId>tools.vitruv.framework.vsum</artifactId>
        </dependency>

        <!-- external dependencies -->
        <dependency>
            <groupId>org.eclipse.emf</groupId>
            <artifactId>org.eclipse.emf.ecore</artifactId>
        </dependency>

        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-api</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
</#noparse>