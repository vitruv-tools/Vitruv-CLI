package tools.vitruv.cli.options;

import java.nio.file.Path;
import java.util.Arrays;
import org.apache.commons.cli.CommandLine;
import tools.vitruv.cli.configuration.VitruvConfiguration;
import tools.vitruv.framework.vsum.VirtualModelBuilder;

/**
 * CLI option for OCL constraint files that are built into the generated VSUM. The option can be
 * repeated to add several files.
 */
public class ConstraintOption extends VitruvCLIOption {
  static final String OPT = "c";
  static final String DIRECTORY_OPT = "cs";

  /** Creates the option. */
  public ConstraintOption() {
    super(OPT, "constraint", true, "The path to an OCL constraint file; can be repeated.");
    this.setRequired(false);
  }

  @Override
  public void prepare(CommandLine cmd, VitruvConfiguration configuration) {
    if (!cmd.hasOption(OPT)) {
      return;
    }
    if (cmd.hasOption(DIRECTORY_OPT)) {
      throw new IllegalArgumentException(
          "Use either -c/--constraint OR -cs/--constraints, not both.");
    }
    configuration.setConstraintLocations(
        ConstraintFiles.locate(Arrays.stream(cmd.getOptionValues(OPT)).map(Path::of).toList()));
  }

  @Override
  public VirtualModelBuilder applyInternal(
      CommandLine cmd, VirtualModelBuilder builder, VitruvConfiguration configuration) {
    ConstraintFiles.copy(configuration.getLocalPath(), configuration.getConstraintLocations());
    return builder;
  }
}
