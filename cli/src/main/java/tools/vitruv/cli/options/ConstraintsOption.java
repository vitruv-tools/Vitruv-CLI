package tools.vitruv.cli.options;

import java.nio.file.Path;
import org.apache.commons.cli.CommandLine;
import tools.vitruv.cli.configuration.VitruvConfiguration;
import tools.vitruv.framework.vsum.VirtualModelBuilder;

/** CLI option for a directory whose OCL constraint files are built into the generated VSUM. */
public class ConstraintsOption extends VitruvCLIOption {

  /** Creates the option. */
  public ConstraintsOption() {
    super(
        ConstraintOption.DIRECTORY_OPT,
        "constraints",
        true,
        "Directory containing .ocl constraint files.");
    this.setRequired(false);
  }

  @Override
  public void prepare(CommandLine cmd, VitruvConfiguration configuration) {
    if (!cmd.hasOption(ConstraintOption.DIRECTORY_OPT)) {
      return;
    }
    if (cmd.hasOption(ConstraintOption.OPT)) {
      throw new IllegalArgumentException(
          "Use either -c/--constraint OR -cs/--constraints, not both.");
    }
    String directory = cmd.getOptionValue(ConstraintOption.DIRECTORY_OPT);
    if (directory == null || directory.isBlank()) {
      throw new IllegalArgumentException("Missing value for -cs/--constraints.");
    }
    configuration.setConstraintLocations(
        ConstraintFiles.locate(ConstraintFiles.list(Path.of(directory))));
  }

  @Override
  public VirtualModelBuilder applyInternal(
      CommandLine cmd, VirtualModelBuilder builder, VitruvConfiguration configuration) {
    ConstraintFiles.copy(configuration.getLocalPath(), configuration.getConstraintLocations());
    return builder;
  }
}
