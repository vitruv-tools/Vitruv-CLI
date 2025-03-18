package tools.vitruv.cli;

import org.apache.commons.cli.CommandLine;
import tools.vitruv.cli.configuration.VitruvConfiguration;
import tools.vitruv.framework.vsum.VirtualModelBuilder;

/**
 * The VirtualModelBuilderApplication interface is used to define the application that is used to
 * build the virtual model.
 */
public interface VirtualModelBuilderApplication {
  /**
   * The prepare method is used to prepare the application for the build process. It is called
   * before the build process is started.
   *
   * @param cmd The command line arguments.
   * @param configuration The configuration of the application.
   */
  void prepare(CommandLine cmd, VitruvConfiguration configuration);

  /**
   * The preBuild method is called before the build process is started. It can be used to modify the
   * VirtualModelBuilder before the build process is started.
   *
   * @param cmd The command line arguments.
   * @param builder The VirtualModelBuilder that is used to build the virtual model.
   * @param configuration The configuration of the application.
   * @return The modified VirtualModelBuilder.
   */
  VirtualModelBuilder preBuild(
      CommandLine cmd, VirtualModelBuilder builder, VitruvConfiguration configuration);

  /**
   * The postBuild method is called after the build process is finished. It can be used to modify
   * the
   *
   * @param cmd The command line arguments.
   * @param builder The VirtualModelBuilder that is used to build the virtual model.
   * @param configuration The configuration of the application.
   * @return The modified VirtualModelBuilder.
   */
  VirtualModelBuilder postBuild(
      CommandLine cmd, VirtualModelBuilder builder, VitruvConfiguration configuration);
}
