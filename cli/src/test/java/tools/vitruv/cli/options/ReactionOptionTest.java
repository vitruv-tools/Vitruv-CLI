package tools.vitruv.cli.options;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.vitruv.cli.configuration.VitruvConfiguration;
import tools.vitruv.framework.vsum.VirtualModelBuilder;

class ReactionOptionTest {

  @Test
  void repeatedReactionOptionCopiesEveryReactionFile(@TempDir Path tempDir) throws Exception {
    Path firstReaction =
        Files.writeString(tempDir.resolve("first.reactions"), "reactions: first");
    Path secondReaction =
        Files.writeString(tempDir.resolve("second.reactions"), "reactions: second");
    Path output = tempDir.resolve("output");

    Options options = new Options();
    options.addOption(new FolderOption());
    options.addOption(new ReactionOption());
    CommandLine commandLine =
        new DefaultParser()
            .parse(
                options,
                new String[] {
                  "-f", output.toString(),
                  "-r", firstReaction.toString(),
                  "-r", secondReaction.toString()
                });

    Arrays.stream(commandLine.getOptions())
        .filter(ReactionOption.class::isInstance)
        .map(ReactionOption.class::cast)
        .forEach(
            option ->
                option.preBuild(
                    commandLine, new VirtualModelBuilder(), new VitruvConfiguration()));

    Path copiedReactions = output.resolve("consistency/src/main/reactions");
    assertThat(copiedReactions.resolve(firstReaction.getFileName()))
        .hasSameTextualContentAs(firstReaction);
    assertThat(copiedReactions.resolve(secondReaction.getFileName()))
        .hasSameTextualContentAs(secondReaction);
  }
}
