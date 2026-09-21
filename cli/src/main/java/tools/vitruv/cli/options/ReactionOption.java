package tools.vitruv.cli.options;

import java.nio.file.Path;
import java.util.List;

import org.apache.commons.cli.CommandLine;

import tools.vitruv.cli.configuration.VitruvConfiguration;
import tools.vitruv.framework.vsum.VirtualModelBuilder;

/**
 * CLI option handler for configuring change propagation reactions in the Vitruv framework.
 */
public class ReactionOption extends VitruvCLIOption {

  public ReactionOption() {
    super("r", "reaction", true, "The path to the file the Reactions are stored in.");
    this.setRequired(false);
  }

  @Override
  public VirtualModelBuilder applyInternal(
      CommandLine cmd, VirtualModelBuilder builder, VitruvConfiguration configuration) {
    if (!cmd.hasOption(getOpt())) {
      return builder;
    }
    String reactionsPath = cmd.getOptionValue(getOpt());
    FileUtils.copyFile(
        reactionsPath, getPath(cmd, builder), "/consistency/src/main/reactions/");
    return builder;
  }

  @Override
  public VirtualModelBuilder postBuild(
      CommandLine cmd, VirtualModelBuilder builder, VitruvConfiguration configuration) {
    if (!cmd.hasOption(getOpt())) {
      return builder;
    }
    return loadChangePropagationSpecifications(cmd, builder, configuration);
  }

  @Override
  public void prepare(CommandLine cmd, VitruvConfiguration configuration) {
    if (!cmd.hasOption(getOpt())) {
      return;
    }
    configuration.setReactionLocations(
        List.of(Path.of(cmd.getOptionValue(getOpt()).trim()).toAbsolutePath()));
  }
}
