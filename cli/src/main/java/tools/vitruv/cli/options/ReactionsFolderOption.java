package tools.vitruv.cli.options;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.cli.CommandLine;

import tools.vitruv.change.propagation.ChangePropagationSpecification;
import tools.vitruv.cli.configuration.VitruvConfiguration;
import tools.vitruv.framework.vsum.VirtualModelBuilder;

public class ReactionsFolderOption extends ReactionsOption {
  private List<File> allReactionsFiles = new ArrayList<>();
  private static FileFilter filterForReactionsFiles = new FileFilter() {
    @Override
    public boolean accept(File pathname) {
      return pathname.isFile() && pathname.getAbsolutePath().endsWith(".reactions");
    }
  };

  /**
   * Creates the ReactionsOption.
   */
  public ReactionsFolderOption() {
    super("rs", "reactions", true, "The path to the folder the Reactions files are stored in.");
  }

  @Override
  public VirtualModelBuilder applyInternal(CommandLine cmd, VirtualModelBuilder builder,
      VitruvConfiguration configuration) {
    String reactionsFolderPath = cmd.getOptionValue(getOpt());
    // We should open a directory, and add all files ending with .reactions
    File reactionsFolder = FileUtils.getFile(reactionsFolderPath);
    if (!reactionsFolder.isDirectory()) { 
      throw new UnsupportedOperationException("Functionality not implemented yet!");
    }
    for (File containedFile : reactionsFolder.listFiles(filterForReactionsFiles)) {
      var reactionsFilePath = containedFile.getAbsolutePath();
      File reactionsFile = FileUtils.copyFile(
          reactionsFilePath, getPath(cmd, builder), ReactionsOption.REACTIONS_FILE_PATH);
      allReactionsFiles.add(reactionsFile);
    }
    return builder;
  }

  @Override
  public VirtualModelBuilder postBuild(CommandLine cmd, VirtualModelBuilder builder,
          VitruvConfiguration configuration) {
    registerJarsToClasspath(cmd, builder);
    List<ChangePropagationSpecification> specifications = allReactionsFiles
        .stream()
        .map(reactionsFile -> getCPSForReactionsFile(reactionsFile, cmd, builder))
        .toList();
    return builder.withChangePropagationSpecifications(specifications);
  }

  @Override
  public void prepare(CommandLine cmd, VitruvConfiguration configuration) {}
}
