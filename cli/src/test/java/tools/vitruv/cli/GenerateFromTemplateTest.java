package tools.vitruv.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.vitruv.cli.exceptions.MissingModelException;

class GenerateFromTemplateTest {
  private static final String PACKAGE_NAME = "my.example";
  private static final List<String> SPECIFICATIONS =
      List.of(
          "mir.reactions.aToB.AToBChangePropagationSpecification",
          "mir.reactions.bToA.BToAChangePropagationSpecification");

  @TempDir Path tempDir;

  private final GenerateFromTemplate generator = new GenerateFromTemplate();

  @Test
  void generateRootPom_shouldFail_whenPackageNameIsMissing() {
    assertThatThrownBy(() -> generator.generateRootPom(tempDir.resolve("pom.xml").toFile(), null))
        .isInstanceOf(MissingModelException.class);
    assertThatThrownBy(() -> generator.generateRootPom(tempDir.resolve("pom.xml").toFile(), ""))
        .isInstanceOf(MissingModelException.class);
  }

  @Test
  void generatePoms_shouldUsePackageNameForArtifacts() throws Exception {
    Path rootPom = tempDir.resolve("pom.xml");
    Path vsumPom = tempDir.resolve("vsum/pom.xml");
    Path modelPom = tempDir.resolve("model/pom.xml");
    Path consistencyPom = tempDir.resolve("consistency/pom.xml");

    generator.generateRootPom(rootPom.toFile(), PACKAGE_NAME);
    generator.generateVsumPom(vsumPom.toFile(), PACKAGE_NAME);
    generator.generateModelPom(modelPom.toFile(), PACKAGE_NAME);
    generator.generateConsistencyPom(consistencyPom.toFile(), PACKAGE_NAME);

    assertThat(rootPom).content().contains("<artifactId>my.example</artifactId>");
    assertThat(vsumPom).content().contains("<artifactId>my.example.vsum</artifactId>");
    assertThat(modelPom).content().contains("<artifactId>my.example.model</artifactId>");
    assertThat(consistencyPom)
        .content()
        .contains("<artifactId>my.example.consistency</artifactId>")
        .doesNotContain("${packageName}");
  }

  @Test
  void generateVsumExample_shouldRegisterAllSpecificationsAndViewTypes() throws Exception {
    Path example = tempDir.resolve("VSUMExample.java");

    generator.generateVsumExample(
        example.toFile(), PACKAGE_NAME, List.of("first", "second"), SPECIFICATIONS);

    assertThat(example)
        .content()
        .contains("package my.example.vsum;")
        .contains("import mir.reactions.aToB.AToBChangePropagationSpecification;")
        .contains("import mir.reactions.bToA.BToAChangePropagationSpecification;")
        .contains(
            ".withChangePropagationSpecifications("
                + "new AToBChangePropagationSpecification(), "
                + "new BToAChangePropagationSpecification())")
        .contains("createIdentityMappingViewType(\"first\")")
        .contains("createIdentityMappingViewType(\"second\")");
  }

  @Test
  void generateVsumTest_shouldBeEnabledAndUseAllSpecifications() throws Exception {
    Path test = tempDir.resolve("VSUMExampleTest.java");

    generator.generateVsumTest(test.toFile(), PACKAGE_NAME, SPECIFICATIONS);

    assertThat(test)
        .content()
        .contains("package my.example.vsum;")
        .contains("import mir.reactions.aToB.AToBChangePropagationSpecification;")
        .contains(
            ".withChangePropagationSpecifications("
                + "new AToBChangePropagationSpecification(), "
                + "new BToAChangePropagationSpecification())")
        .doesNotContain("@Disabled");
  }

  @Test
  void generateVsumTest_shouldSupportConfigurationsWithoutReactions() throws Exception {
    Path test = tempDir.resolve("VSUMExampleTest.java");

    generator.generateVsumTest(test.toFile(), PACKAGE_NAME, List.of());

    assertThat(test)
        .content()
        .contains(".withChangePropagationSpecifications()")
        .doesNotContain("mir.reactions")
        .doesNotContain("${");
  }
}
