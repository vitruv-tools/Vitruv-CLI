package tools.vitruv.cli.options;

import java.io.File;
import org.apache.commons.cli.CommandLine;
import tools.vitruv.change.propagation.ChangePropagationSpecification;
import tools.vitruv.cli.configuration.CustomClassLoader;
import tools.vitruv.cli.configuration.VitruvConfiguration;
import tools.vitruv.framework.vsum.VirtualModelBuilder;

/**
 * Option to add a single Reactions file to the V-SUMM to build.
 */
public class ReactionsFileOption extends ReactionsOption {
  private File reactionsFile;

  /**
   * Creates the ReactionsFileOption.
   *
   * @param classLoader - {@link CustomClassLoader}
   */
  public ReactionsFileOption(CustomClassLoader classLoader) {
    super("r", "reaction", true, "The path to the file the Reactions are stored in.", classLoader);
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
    registerJarsToClasspath(cmd, builder);
    ChangePropagationSpecification loadedClass = getCPSForReactionsFile(
        reactionsFile, cmd, builder);
    return builder.withChangePropagationSpecification(loadedClass);
  }



  @Override
  public void prepare(CommandLine cmd, VitruvConfiguration configuration) {}

}
