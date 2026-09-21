package tools.vitruv.cli.options;

import java.nio.file.Path;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import tools.vitruv.change.propagation.ChangePropagationSpecification;
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
  protected VitruvCLIOption(
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
    return new FolderOption().getPath(cmd, builder);
  }

  @Override
  public VirtualModelBuilder preBuild(
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

  /**
   * Loads the change propagation specifications that were generated for the configured reaction
   * files from the built project and registers them at the given builder.
   *
   * @param cmd The command line arguments.
   * @param builder The VirtualModelBuilder that is used to build the virtual model.
   * @param configuration The configuration of the application.
   * @return The modified VirtualModelBuilder.
   */
  protected VirtualModelBuilder loadChangePropagationSpecifications(
      CommandLine cmd, VirtualModelBuilder builder, VitruvConfiguration configuration) {
    Path projectPath = getPath(cmd, builder).toAbsolutePath();
    String packageName = configuration.getPackageName();
    FileUtils.addJarToClassPath(
        projectPath.resolve("model/target/" + packageName + ".model-0.1.0-SNAPSHOT.jar").toString());
    FileUtils.addJarToClassPath(
        projectPath
            .resolve("consistency/target/" + packageName + ".consistency-0.1.0-SNAPSHOT.jar")
            .toString());
    for (String specificationName : configuration.getChangePropagationSpecificationNames()) {
      try {
        builder.withChangePropagationSpecification(
            (ChangePropagationSpecification)
                FileUtils.CLASS_LOADER
                    .loadClass(specificationName)
                    .getDeclaredConstructor()
                    .newInstance());
      } catch (ReflectiveOperationException e) {
        throw new IllegalStateException(
            "Could not load change propagation specification " + specificationName, e);
      }
    }
    return builder;
  }

  public abstract VirtualModelBuilder applyInternal(
      CommandLine cmd, VirtualModelBuilder builder, VitruvConfiguration configuration);
}
