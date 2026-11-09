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
import tools.vitruv.cli.configuration.ProjectVersions;
import tools.vitruv.cli.configuration.VitruvConfiguration;
import tools.vitruv.framework.vsum.VirtualModelBuilder;

class ConstraintOptionTest {

  @TempDir Path tempDir;

  ConstraintOption option = new ConstraintOption();
  VitruvConfiguration config;
  Path project;

  @BeforeEach
  void setup() {
    project = tempDir.resolve("project");
    config = new VitruvConfiguration();
    config.setLocalPath(project);
  }

  private CommandLine parse(String... args) throws Exception {
    Options options = new Options();
    options.addOption(option);
    options.addOption(new ConstraintsOption());
    return new DefaultParser().parse(options, args);
  }

  private Path ocl(String relativePath) throws Exception {
    Path file = tempDir.resolve(relativePath);
    Files.createDirectories(file.getParent());
    return Files.writeString(file, "context model::Component inv named: self.name <> ''");
  }

  @Test
  void prepare_doesNothing_whenOptionNotPresent() throws Exception {
    option.prepare(parse(), config);

    assertThat(config.getConstraintLocations()).isEmpty();
    assertThat(config.getProjectVersions()).isEqualTo(ProjectVersions.DEFAULT);
  }

  @Test
  void prepare_collectsRepeatedFiles_andGivesThemDistinctNames() throws Exception {
    Path prices = ocl("a/prices.ocl");
    Path otherPrices = ocl("b/Prices.ocl");
    Path names = ocl("names.OCL");

    option.prepare(
        parse("-c", prices.toString(), "-c", otherPrices.toString(), "-c", names.toString()),
        config);

    assertThat(config.getConstraintLocations())
        .extracting(ConstraintLocation::fileName)
        .containsExactly("prices.ocl", "Prices-1.ocl", "names.OCL");
    assertThat(config.getConstraintResources())
        .containsExactly("constraints/prices.ocl", "constraints/Prices-1.ocl", "constraints/names.OCL");
    assertThat(config.getProjectVersions()).isEqualTo(ProjectVersions.WITH_CONSTRAINTS);
  }

  @Test
  void prepare_fails_whenFileDoesNotExist() throws Exception {
    CommandLine cmd = parse("-c", tempDir.resolve("missing.ocl").toString());

    assertThatThrownBy(() -> option.prepare(cmd, config))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("does not exist");
  }

  @Test
  void prepare_fails_whenFileIsNoOclFile() throws Exception {
    Path text = Files.writeString(tempDir.resolve("constraints.txt"), "x");
    CommandLine cmd = parse("-c", text.toString());

    assertThatThrownBy(() -> option.prepare(cmd, config))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("must end with .ocl");
  }

  @Test
  void prepare_fails_whenFileIsEmpty() throws Exception {
    Path empty = Files.writeString(tempDir.resolve("empty.ocl"), "");
    CommandLine cmd = parse("-c", empty.toString());

    assertThatThrownBy(() -> option.prepare(cmd, config))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("is empty");
  }

  @Test
  void prepare_fails_whenCombinedWithDirectoryOption() throws Exception {
    CommandLine cmd = parse("-c", ocl("a.ocl").toString(), "-cs", tempDir.toString());

    assertThatThrownBy(() -> option.prepare(cmd, config))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("not both");
  }

  @Test
  void preBuild_copiesFilesIntoTheResourcesOfTheVsum() throws Exception {
    Path prices = ocl("a/prices.ocl");
    Path otherPrices = ocl("b/prices.ocl");
    CommandLine cmd = parse("-c", prices.toString(), "-c", otherPrices.toString());
    option.prepare(cmd, config);

    option.preBuild(cmd, new VirtualModelBuilder(), config);

    Path resources = project.resolve("vsum/src/main/resources/constraints");
    assertThat(resources.resolve("prices.ocl")).hasSameTextualContentAs(prices);
    assertThat(resources.resolve("prices-1.ocl")).hasSameTextualContentAs(otherPrices);
  }
}
