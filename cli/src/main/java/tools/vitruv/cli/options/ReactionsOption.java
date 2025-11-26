package tools.vitruv.cli.options;

import java.io.File;

import org.apache.commons.cli.CommandLine;

import tools.vitruv.change.propagation.ChangePropagationSpecification;
import tools.vitruv.cli.configuration.CustomClassLoader;
import tools.vitruv.framework.vsum.VirtualModelBuilder;

/**
 * Common functionality to add one or more Reactions to the
 * V-SUM to build.
 */
abstract class ReactionsOption extends VitruvCLIOption {
  private final CustomClassLoader classLoader;
  /**
   * File path to store generated reactions file in.
   */
  public static final String REACTIONS_FILE_PATH = "/consistency/src/main/reactions/";

  protected ReactionsOption(String abbreviationName, String longName,
      boolean hasArguments, String description, CustomClassLoader classLoader) {
    super(abbreviationName, longName, hasArguments, description);
    this.classLoader = classLoader;
  }

  protected ChangePropagationSpecification getCPSForReactionsFile(File reactionsFile,
      CommandLine cmd, VirtualModelBuilder builder) {
    ChangePropagationSpecification loadedClass = null;
    try {
      String name = FileUtils.findOption(reactionsFile, "reactions:");
      System.out.println(
          name.substring(0, 1).toUpperCase()
              + name.substring(1)
              + "ChangePropagationSpecification");
      loadedClass =
          (ChangePropagationSpecification)
              classLoader
                  .loadClass(
                      "mir.reactions."
                          + name
                          + "."
                          + name.substring(0, 1).toUpperCase()
                          + name.substring(1)
                          + "ChangePropagationSpecification")
                  .getDeclaredConstructor()
                  .newInstance();
    } catch (Exception e) {
      e.printStackTrace();
    }
    // TODO extract the name of the generated reaction, find that, load that, and
    // add that to the
    // classpath as well as the builder
    return loadedClass;
  }

  protected void registerJarsToClasspath(CommandLine cmd, VirtualModelBuilder builder) {
    classLoader.addJarToClassPath(
        new File("").getAbsolutePath().toString()
            + "/"
            + getPath(cmd, builder)
            + "/model/target/tools.vitruv.methodologisttemplate.model-0.1.0-SNAPSHOT.jar");
    classLoader.addJarToClassPath(
        new File("").getAbsolutePath().toString()
            + "/"
            + getPath(cmd, builder)
            + "/consistency/target/tools.vitruv.methodologisttemplate.consistency-0.1.0-SNAPSHOT.jar");
  }
}
