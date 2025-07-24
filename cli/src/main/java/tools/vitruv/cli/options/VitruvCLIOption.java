package tools.vitruv.cli.options;

import java.nio.file.Path;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import tools.vitruv.cli.VirtualModelBuilderApplication;
import tools.vitruv.cli.configuration.VitruvConfiguration;
import tools.vitruv.framework.vsum.VirtualModelBuilder;

/**
 * The VitruvCLIOption class is used to define the options that are used in the command line
 * interface of the Vitruv framework.
 */
public abstract class VitruvCLIOption extends Option implements VirtualModelBuilderApplication {
  /**
   * The constructor of the VitruvCLIOption class.
   *
   * @param abbreviationName The abbreviation name of the option.
   * @param longName The long name of the option.
   * @param hasArguments A flag that indicates if the option has arguments.
   * @param description The description of the option.
   */
  public VitruvCLIOption(
      String abbreviationName, String longName, boolean hasArguments, String description) {
    super(abbreviationName, longName, hasArguments, description);
  }

  /**
   * The constructor of the VitruvCLIOption class.
   *
   * @param cmd The command line arguments.
   * @param builder The VirtualModelBuilder that is used to build the virtual model.
   * @return The path that is defined by the option.
   */
  public Path getPath(CommandLine cmd, VirtualModelBuilder builder) {
    return Path.of(cmd.getOptionValue(getOpt().replaceAll("\\s", "")));
  }

  @Override
  public final VirtualModelBuilder preBuild(
      CommandLine cmd, VirtualModelBuilder builder, VitruvConfiguration configuration) {
    if (!cmd.hasOption(getOpt())) {
      throw new IllegalArgumentException("Command called but not present!");
    }
    return applyInternal(cmd, builder, configuration);
  }

  @Override
  public VirtualModelBuilder postBuild(
      CommandLine cmd, VirtualModelBuilder builder, VitruvConfiguration configuration) {
    // the default operation is doing nothing after the maven build
    return builder;
  }

  public abstract VirtualModelBuilder applyInternal(
      CommandLine cmd, VirtualModelBuilder builder, VitruvConfiguration configuration);
}
