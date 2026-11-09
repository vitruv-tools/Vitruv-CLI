package tools.vitruv.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.vitruv.cli.configuration.ProjectVersions;

class GenerateFromTemplateConstraintsTest {
  private static final String PACKAGE = "tools.vitruv.methodologisttemplate";

  @TempDir Path tempDir;

  GenerateFromTemplate generator = new GenerateFromTemplate();

  private String generate(String name, Generation generation) throws Exception {
    File file = tempDir.resolve(name).toFile();
    generation.run(file);
    return Files.readString(file.toPath());
  }

  @Test
  void projectsWithoutConstraintsUseCompatibleVersions() throws Exception {
    String rootPom =
        generate("pom.xml", f -> generator.generateRootPom(f, PACKAGE, ProjectVersions.DEFAULT));
    String vsumPom =
        generate("vsum.xml", f -> generator.generateVsumPom(f, PACKAGE, ProjectVersions.DEFAULT));
    String example =
        generate(
            "VSUMExample.java",
            f -> generator.generateVsumExample(f, PACKAGE, List.of("model"), List.of(), List.of()));

    assertThat(rootPom)
        .contains("<version>4.0.2</version>", "<vitruv.version>4.0.2-SNAPSHOT</vitruv.version>")
        .contains("<vitruv-reactions-language.version>4.0.1</vitruv-reactions-language.version>")
        .contains("<junit.version>6.1.3</junit.version>")
        .contains("<vitruv-server.version>4.0.1-SNAPSHOT</vitruv-server.version>", "central-snapshots")
        .doesNotContain("vitruvocl");
    assertThat(vsumPom).doesNotContain("vitruvocl");
    assertThat(example)
        .doesNotContain("VitruvOclConstraintChecker", "withConstraintChecker")
        .contains(".buildAndInitialize();");
  }

  @Test
  void projectsWithConstraintsUseVitruvOcl() throws Exception {
    String rootPom =
        generate(
            "pom.xml", f -> generator.generateRootPom(f, PACKAGE, ProjectVersions.WITH_CONSTRAINTS));
    String vsumPom =
        generate(
            "vsum.xml",
            f -> generator.generateVsumPom(f, PACKAGE, ProjectVersions.WITH_CONSTRAINTS));
    String example =
        generate(
            "VSUMExample.java",
            f ->
                generator.generateVsumExample(
                    f,
                    PACKAGE,
                    List.of("model"),
                    List.of(),
                    List.of("constraints/prices.ocl", "constraints/Größen \"v1\".ocl")));

    assertThat(rootPom)
        .contains("<version>4.0.0</version>", "<vitruv.version>4.0.2-SNAPSHOT</vitruv.version>")
        .contains("<vitruvocl.version>0.1.0-SNAPSHOT</vitruvocl.version>")
        .contains("<junit.version>6.1.0</junit.version>")
        .contains("<artifactId>tools.vitruv.vitruvocl.language</artifactId>", "<version>${vitruvocl.version}</version>")
        .contains("central-snapshots");
    assertThat(vsumPom).contains("<artifactId>tools.vitruv.vitruvocl.language</artifactId>");
    assertThat(example)
        .contains("import tools.vitruv.dsls.vitruvocl.vitruv.VitruvOclConstraintChecker;")
        .contains(
            ".withConstraintChecker(VitruvOclConstraintChecker.fromClasspath("
                + "\"constraints/prices.ocl\", \"constraints/Größen \\\"v1\\\".ocl\"))")
        .contains("throw new UncheckedIOException");
  }

  @Test
  void vsumExampleRunsWithoutInput() throws Exception {
    String example =
        generate(
            "VSUMExample.java",
            f -> generator.generateVsumExample(f, PACKAGE, List.of("model"), List.of(), List.of()));

    assertThat(example)
        .contains("Runtime.getRuntime().addShutdownHook(", "while (scanner.hasNextLine())")
        .contains("reader.setDaemon(true);")
        .doesNotContain("while (!scanner.nextLine().equals(\"quit\"))");
  }

  @FunctionalInterface
  private interface Generation {
    void run(File file) throws Exception;
  }
}
