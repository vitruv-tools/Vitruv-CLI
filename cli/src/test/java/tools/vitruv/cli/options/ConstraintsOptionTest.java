package tools.vitruv.cli.options;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.vitruv.cli.configuration.ConstraintLocation;
import tools.vitruv.cli.configuration.VitruvConfiguration;
import tools.vitruv.framework.vsum.VirtualModelBuilder;

class ConstraintsOptionTest {

  @TempDir Path tempDir;

  ConstraintsOption option = new ConstraintsOption();
  VitruvConfiguration config;
  Path constraints;

  @BeforeEach
  void setup() throws Exception {
    config = new VitruvConfiguration();
    config.setLocalPath(tempDir.resolve("project"));
    constraints = Files.createDirectories(tempDir.resolve("constraints"));
  }

  private CommandLine parse(String... args) throws Exception {
    Options options = new Options();
    options.addOption(option);
    options.addOption(new ConstraintOption());
    return new DefaultParser().parse(options, args);
  }

  @Test
  void prepare_collectsTheOclFilesOfTheDirectorySortedByName() throws Exception {
    Files.writeString(constraints.resolve("b.ocl"), "context model::Link inv b: true");
    Files.writeString(constraints.resolve("a.ocl"), "context model::Link inv a: true");
    Files.writeString(constraints.resolve("notes.txt"), "ignored");
    Files.createDirectories(constraints.resolve("nested.ocl"));

    option.prepare(parse("-cs", constraints.toString()), config);

    assertThat(config.getConstraintLocations())
        .extracting(ConstraintLocation::fileName)
        .containsExactly("a.ocl", "b.ocl");
  }

  @Test
  void prepare_fails_whenDirectoryHasNoOclFiles() throws Exception {
    Files.writeString(constraints.resolve("notes.txt"), "x");
    CommandLine cmd = parse("-cs", constraints.toString());

    assertThatThrownBy(() -> option.prepare(cmd, config))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("No .ocl files");
  }

  @Test
  void prepare_fails_whenDirectoryDoesNotExist() throws Exception {
    CommandLine cmd = parse("-cs", tempDir.resolve("missing").toString());

    assertThatThrownBy(() -> option.prepare(cmd, config))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("does not exist");
  }

  @Test
  void prepare_fails_whenPathIsAFile() throws Exception {
    Path file = Files.writeString(tempDir.resolve("a.ocl"), "context model::Link inv a: true");
    CommandLine cmd = parse("-cs", file.toString());

    assertThatThrownBy(() -> option.prepare(cmd, config))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("not a directory");
  }

  @Test
  void prepare_fails_whenValueIsBlank() throws Exception {
    CommandLine cmd = parse("-cs", " ");

    assertThatThrownBy(() -> option.prepare(cmd, config))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Missing value for -cs/--constraints");
  }

  @Test
  void preBuild_copiesTheFilesIntoTheResourcesOfTheVsum() throws Exception {
    Path a = Files.writeString(constraints.resolve("a.ocl"), "context model::Link inv a: true");
    CommandLine cmd = parse("-cs", constraints.toString());
    option.prepare(cmd, config);

    option.preBuild(cmd, new VirtualModelBuilder(), config);

    assertThat(config.getLocalPath().resolve("vsum/src/main/resources/constraints/a.ocl"))
        .hasSameTextualContentAs(a);
  }
}
