package tools.vitruv.cli.options;

import java.io.IOException;
import java.nio.file.*;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.cli.CommandLine;
import tools.vitruv.cli.configuration.VitruvConfiguration;
import tools.vitruv.framework.vsum.VirtualModelBuilder;

public class ReactionsOption extends VitruvCLIOption {

  private static final String SINGLE_REACTION_OPT = "r";
  private static final String MULTI_REACTIONS_OPT = "rs";

  public ReactionsOption() {
    super(MULTI_REACTIONS_OPT, "reactions", false, "Directory containing .reactions files.");
    this.setArgs(1);
    this.setOptionalArg(false);
  }

  @Override
  public void prepare(CommandLine cmd, VitruvConfiguration configuration) {
    if (!cmd.hasOption(MULTI_REACTIONS_OPT)) return;

    if (cmd.hasOption(SINGLE_REACTION_OPT)) {
      throw new IllegalArgumentException("Use either -r/--reaction OR -rs/--reactions, not both.");
    }

    String dirValue = cmd.getOptionValue(MULTI_REACTIONS_OPT);
    if (dirValue == null || dirValue.isBlank()) {
      throw new IllegalArgumentException("Missing value for -rs/--reactions.");
    }

    Path reactionsDir = Path.of(dirValue).toAbsolutePath().normalize();
    if (!Files.exists(reactionsDir)) {
      throw new IllegalArgumentException("Reactions directory does not exist: " + reactionsDir);
    }
    if (!Files.isDirectory(reactionsDir)) {
      throw new IllegalArgumentException("Reactions path is not a directory: " + reactionsDir);
    }

    List<Path> reactionFiles = listReactionFiles(reactionsDir);
    if (reactionFiles.isEmpty()) {
      throw new IllegalArgumentException("No .reactions files found in directory: " + reactionsDir);
    }

    configuration.setReactionLocations(reactionFiles);
  }

  @Override
  public VirtualModelBuilder preBuild(
          CommandLine cmd, VirtualModelBuilder builder, VitruvConfiguration configuration) {

    if (!cmd.hasOption(MULTI_REACTIONS_OPT)) return builder;

    Path targetDir = configuration.getLocalPath().resolve("consistency/src/main/reactions");

    try {
      Files.createDirectories(targetDir);
      for (Path src : configuration.getReactionLocations()) {
        Files.copy(src, targetDir.resolve(src.getFileName()), StandardCopyOption.REPLACE_EXISTING);
      }
      return builder;
    } catch (IOException e) {
      throw new IllegalStateException("Failed to copy reaction files into project: " + e.getMessage(), e);
    }
  }

  @Override
  public VirtualModelBuilder applyInternal(
          CommandLine cmd, VirtualModelBuilder builder, VitruvConfiguration configuration) {
    return builder;
  }

  private List<Path> listReactionFiles(Path reactionsDir) {
    try (var stream = Files.list(reactionsDir)) {
      return stream
              .filter(Files::isRegularFile)
              .filter(p -> p.getFileName().toString().endsWith(".reactions"))
              .sorted(Comparator.comparing(p -> p.getFileName().toString()))
              .toList();
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read reactions directory: " + reactionsDir, e);
    }
  }
}
