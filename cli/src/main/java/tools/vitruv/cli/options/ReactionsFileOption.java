package tools.vitruv.cli.options;

import java.io.File;
import org.apache.commons.cli.CommandLine;
import tools.vitruv.change.propagation.ChangePropagationSpecification;
import tools.vitruv.cli.configuration.VitruvConfiguration;
import tools.vitruv.framework.vsum.VirtualModelBuilder;

public class ReactionsFileOption extends ReactionsOption {
  private File reactionsFile;

  public ReactionsFileOption() {
    super("r", "reaction", true, "The path to the file the Reactions are stored in.");
  }

  @Override
  public VirtualModelBuilder applyInternal(
      CommandLine cmd, VirtualModelBuilder builder, VitruvConfiguration configuration) {
    String reactionsPath = cmd.getOptionValue(getOpt());
    reactionsFile =
        FileUtils.copyFile(
            reactionsPath, getPath(cmd, builder), ReactionsOption.REACTIONS_FILE_PATH);
    return builder;
  }

  @Override
  public VirtualModelBuilder postBuild(
      CommandLine cmd, VirtualModelBuilder builder, VitruvConfiguration configuration) {
    ChangePropagationSpecification loadedClass = getCPSForReactionsFile(
        reactionsFile, cmd, builder);
    return builder.withChangePropagationSpecification(loadedClass);
  }



  @Override
  public void prepare(CommandLine cmd, VitruvConfiguration configuration) {}

}
