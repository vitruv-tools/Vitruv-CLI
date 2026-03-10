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

  /**
   * No-op implementation.
   * <p>
   * This option is a flag that modifies behavior of other options (like GenmodelPrecheckOption).
   * It does not require any preparation logic of its own.
   */
  @Override
  public void prepare(CommandLine cmd, VitruvConfiguration configuration) {}

  /**
   * No-op implementation.
   * <p>
   * This option does not modify the builder before virtual model construction.
   */
  @Override
  public VirtualModelBuilder preBuild(
      CommandLine cmd, VirtualModelBuilder builder, VitruvConfiguration configuration) {
    return builder;
  }

  /**
   * No-op implementation.
   * <p>
   * This option does not modify the builder during internal application phase.
   */
  @Override
  public VirtualModelBuilder applyInternal(
      CommandLine cmd, VirtualModelBuilder builder, VitruvConfiguration configuration) {
    return builder;
  }

  /**
   * No-op implementation.
   * <p>
   * This option does not modify the builder after virtual model construction.
   */
  @Override
  public VirtualModelBuilder postBuild(
      CommandLine cmd, VirtualModelBuilder builder, VitruvConfiguration configuration) {
    return builder;
  }
}
