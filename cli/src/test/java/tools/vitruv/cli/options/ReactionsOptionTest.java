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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import tools.vitruv.cli.configuration.VitruvConfiguration;
import tools.vitruv.framework.vsum.VirtualModelBuilder;

@ExtendWith(MockitoExtension.class)
class ReactionsOptionTest {

  ReactionsOption option = new ReactionsOption();

  VitruvConfiguration config;
  VirtualModelBuilder builder;

  @BeforeEach
  void setup(@TempDir Path tempDir) {
    config = new VitruvConfiguration();
    config.setLocalPath(tempDir);
    builder = new VirtualModelBuilder();
  }

  private CommandLine parse(String... args) throws Exception {
    Options options = new Options();
    options.addOption(option);
    options.addOption(new ReactionOption());
    return new DefaultParser().parse(options, args);
  }

  @Test
  void prepare_shouldDoNothing_whenOptionNotPresent() throws Exception {
    CommandLine cmd = parse();

    option.prepare(cmd, config);

    assertThat(config.getReactionLocations()).isEmpty();
  }

  @Test
  void prepare_shouldFail_whenDirectoryDoesNotExist() throws Exception {
    Path non = config.getLocalPath().resolve("does-not-exist");

    CommandLine cmd = parse("-rs", non.toString());

    assertThatThrownBy(() -> option.prepare(cmd, config))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("does not exist");
  }

  @Test
  void prepare_shouldFail_whenPathIsFile() throws Exception {
    Path file = Files.writeString(config.getLocalPath().resolve("some-file.txt"), "x");

    CommandLine cmd = parse("-rs", file.toString());

    assertThatThrownBy(() -> option.prepare(cmd, config))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("not a directory");
  }

  @Test
  void prepare_shouldFail_whenValueIsBlank() throws Exception {
    CommandLine cmd = parse("-rs", "");

    assertThatThrownBy(() -> option.prepare(cmd, config))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Missing value for -rs/--reactions");
  }

  @Test
  void preBuild_shouldThrow_whenTargetIsFile(@TempDir Path reactionsDir) throws Exception {
    Files.writeString(reactionsDir.resolve("a.reactions"), "A");
    Files.writeString(reactionsDir.resolve("b.reactions"), "B");

    CommandLine cmd = parse("-rs", reactionsDir.toString());
    option.prepare(cmd, config);

    Path parent = config.getLocalPath().resolve("consistency/src/main");
    Files.createDirectories(parent);
    Files.createFile(parent.resolve("reactions"));

    assertThatThrownBy(() -> option.preBuild(cmd, builder, config))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Failed to copy reaction files");
  }

  @Test
  void preBuild_shouldReturnSameBuilder_whenOptionNotPresent() throws Exception {
    CommandLine cmd = parse();
    VirtualModelBuilder result = option.preBuild(cmd, builder, config);
    assertThat(result).isSameAs(builder);
  }

  @Test
  void prepare_shouldSortReactionFilesByName(@TempDir Path reactionsDir) throws Exception {
    Files.writeString(reactionsDir.resolve("b.reactions"), "B");
    Files.writeString(reactionsDir.resolve("a.reactions"), "A");

    CommandLine cmd = parse("-rs", reactionsDir.toString());
    option.prepare(cmd, config);

    assertThat(config.getReactionLocations()).hasSize(2);
    assertThat(config.getReactionLocations().get(0).getFileName().toString())
        .isEqualTo("a.reactions");
    assertThat(config.getReactionLocations().get(1).getFileName().toString())
        .isEqualTo("b.reactions");
  }
}
