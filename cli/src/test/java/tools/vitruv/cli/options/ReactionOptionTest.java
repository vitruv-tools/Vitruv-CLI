package tools.vitruv.cli.options;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.vitruv.cli.configuration.VitruvConfiguration;
import tools.vitruv.framework.vsum.VirtualModelBuilder;

class ReactionOptionTest {
  @TempDir Path tempDir;

  ReactionOption option = new ReactionOption();
  FolderOption folderOption = new FolderOption();

  VitruvConfiguration config;
  VirtualModelBuilder builder;

  @BeforeEach
  void setup() {
    config = new VitruvConfiguration();
    builder = new VirtualModelBuilder();
    folderOption.setRequired(false);
  }

  private CommandLine parse(String... args) throws Exception {
    Options options = new Options();
    options.addOption(option);
    options.addOption(folderOption);
    return new DefaultParser().parse(options, args);
  }

  @Test
  void prepare_shouldDoNothing_whenOptionNotPresent() throws Exception {
    option.prepare(parse(), config);

    assertThat(config.getReactionLocations()).isEmpty();
  }

  @Test
  void prepare_shouldRegisterAbsoluteReactionLocation() throws Exception {
    option.prepare(parse("-r", "some/relative.reactions"), config);

    assertThat(config.getReactionLocations())
        .containsExactly(Path.of("some/relative.reactions").toAbsolutePath());
  }

  @Test
  void preBuild_shouldCopyReactionFileIntoConsistencyProject() throws Exception {
    Path reactions = Files.writeString(tempDir.resolve("my.reactions"), "reactions: mine");
    Path project = tempDir.resolve("project");
    CommandLine cmd = parse("-r", reactions.toString(), "-f", project.toString());

    VirtualModelBuilder result = option.preBuild(cmd, builder, config);

    assertThat(result).isSameAs(builder);
    assertThat(project.resolve("consistency/src/main/reactions/my.reactions"))
        .hasContent("reactions: mine");
  }

  @Test
  void postBuild_shouldReturnSameBuilder_whenOptionNotPresent() throws Exception {
    assertThat(option.postBuild(parse(), builder, config)).isSameAs(builder);
  }

  @Test
  void folderOption_shouldConfigureLocalPathAndStorageFolder() throws Exception {
    CommandLine cmd = parse("-f", tempDir.toString());

    folderOption.prepare(cmd, config);

    assertThat(config.getLocalPath()).isEqualTo(tempDir);
    assertThat(folderOption.getPath(cmd, builder)).isEqualTo(tempDir);
    assertThat(folderOption.preBuild(cmd, builder, config)).isSameAs(builder);
  }
}
