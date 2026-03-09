package tools.vitruv.cli.options;

import org.apache.commons.cli.CommandLine;
import tools.vitruv.cli.configuration.VitruvConfiguration;
import tools.vitruv.framework.vsum.VirtualModelBuilder;

/** CLI flag that applies genmodel fixes without interactive confirmation. */
public class ApplyOption extends VitruvCLIOption {

  private static final String OPT = "a";

  /** Constructs the apply option. */
  public ApplyOption() {
    super(OPT, "apply", false, "Apply detected genmodel fixes without asking for confirmation.");
    this.setArgs(0);
  }

  @Override
  public void prepare(CommandLine cmd, VitruvConfiguration configuration) {}

  @Override
  public VirtualModelBuilder preBuild(
      CommandLine cmd, VirtualModelBuilder builder, VitruvConfiguration configuration) {
    return builder;
  }

  @Override
  public VirtualModelBuilder applyInternal(
      CommandLine cmd, VirtualModelBuilder builder, VitruvConfiguration configuration) {
    return builder;
  }

  @Override
  public VirtualModelBuilder postBuild(
      CommandLine cmd, VirtualModelBuilder builder, VitruvConfiguration configuration) {
    return builder;
  }
}
