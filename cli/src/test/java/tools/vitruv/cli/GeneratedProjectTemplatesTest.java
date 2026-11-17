package tools.vitruv.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.commons.cli.ParseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.w3c.dom.Element;
import tools.vitruv.cli.configuration.ProjectVersions;

class GeneratedProjectTemplatesTest {
  @TempDir Path directory;
  private final GenerateFromTemplate generator = new GenerateFromTemplate();

  static Stream<Arguments> combinations() {
    List<Arguments> cases = new ArrayList<>();
    for (int reactions = 0; reactions <= 2; reactions++) {
      for (int constraints = 0; constraints <= 2; constraints++) {
        cases.add(Arguments.of(reactions, constraints));
      }
    }
    return cases.stream();
  }

  @ParameterizedTest
  @MethodSource("combinations")
  void generatedExampleKeepsReactionsAndConstraintsIndependent(int reactions, int constraints)
      throws Exception {
    List<String> specifications = List.of(
        "mir.reactions.first.FirstChangePropagationSpecification",
        "mir.reactions.second.SecondChangePropagationSpecification").subList(0, reactions);
    List<String> resources = List.of(
        "constraints/prices.ocl", "constraints/Größen \"v1\".ocl").subList(0, constraints);
    Path output = directory.resolve("VSUMExample.java");
    generator.generateVsumExample(output.toFile(), "custom.example", List.of("model"),
        specifications, resources);
    String java = Files.readString(output, StandardCharsets.UTF_8);
    assertThat(java).contains("package custom.example.vsum;")
        .doesNotContain("${", "<#", "Model2Model2ChangePropagationSpecification");
    assertThat(java.contains("withConstraintChecker")).isEqualTo(constraints > 0);
    for (String specification : specifications) {
      assertThat(java).contains("import " + specification + ";")
          .contains("new " + specification.substring(specification.lastIndexOf('.') + 1) + "()");
    }
    if (reactions == 0) {
      assertThat(java).contains(".withChangePropagationSpecifications()");
    }
    if (constraints == 2) {
      assertThat(java).contains("constraints/Größen \\\"v1\\\".ocl");
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void generatedPomsManageRuntimeDependenciesWithTheSelectedMainVersions(boolean constraints)
      throws Exception {
    ProjectVersions versions = ProjectVersions.forProject(constraints);
    Path root = directory.resolve("pom.xml");
    Path consistency = directory.resolve("consistency.xml");
    generator.generateRootPom(root.toFile(), "custom.example", versions);
    generator.generateConsistencyPom(consistency.toFile(), "custom.example");
    var document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(root.toFile());
    assertThat(document.getElementsByTagName("vitruv-change.version").item(0).getTextContent())
        .isEqualTo(versions.change());
    assertThat(document.getElementsByTagName("vitruv-reactions-language.version").item(0).getTextContent())
        .isEqualTo(versions.reactionsLanguage());
    for (String artifact : List.of("tools.vitruv.change.utils", "tools.vitruv.change.interaction.model")) {
      boolean managed = false;
      var dependencies = document.getElementsByTagName("dependency");
      for (int i = 0; i < dependencies.getLength(); i++) {
        Element dependency = (Element) dependencies.item(i);
        if (dependency.getElementsByTagName("artifactId").item(0).getTextContent().equals(artifact)) {
          assertThat(dependency.getElementsByTagName("version").item(0).getTextContent())
              .isEqualTo("${vitruv-change.version}");
          managed = true;
        }
      }
      assertThat(managed).as("Managed dependency %s", artifact).isTrue();
    }
    var pluginPom = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(consistency.toFile());
    var dependencies = ((Element) pluginPom.getElementsByTagName("build").item(0))
        .getElementsByTagName("dependency");
    int reactionDependencies = 0;
    for (int i = 0; i < dependencies.getLength(); i++) {
      Element dependency = (Element) dependencies.item(i);
      String artifact = dependency.getElementsByTagName("artifactId").item(0).getTextContent();
      if (List.of("tools.vitruv.dsls.reactions.language", "tools.vitruv.dsls.reactions.runtime",
          "tools.vitruv.dsls.common").contains(artifact)) {
        assertThat(dependency.getElementsByTagName("version").item(0).getTextContent())
            .isEqualTo("${vitruv-reactions-language.version}");
        reactionDependencies++;
      }
    }
    assertThat(reactionDependencies).isEqualTo(3);
  }

  @Test
  void incompatibleConstraintOptionsFailBeforeCreatingFiles() {
    String[] args = {"-m", "unused.ecore,unused.genmodel", "-f", directory.resolve("project").toString(),
        "-u", "default", "-c", "unused.ocl", "-cs", "unused-directory"};
    assertThatThrownBy(() -> new CLI().run(args)).isInstanceOf(ParseException.class)
        .hasMessageContaining("mutually exclusive");
    assertThat(directory.resolve("project")).doesNotExist();
  }
}
