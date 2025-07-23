package tools.vitruv.cli;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import tools.vitruv.cli.configuration.VitruvConfiguration;
import tools.vitruv.cli.options.FolderOption;
import tools.vitruv.cli.options.MetamodelOption;
import tools.vitruv.cli.options.ReactionOption;
import tools.vitruv.cli.options.UserInteractorOption;
import tools.vitruv.cli.options.VitruvCLIOption;
import tools.vitruv.framework.vsum.VirtualModelBuilder;
import java.util.logging.Logger;

/**
 * The CLI class is the main entry point for the command line interface of the
 * Vitruv framework. It
 * parses the command line arguments and triggers the generation of the
 * necessary files and the
 * build of the project.
 */
public class CLI {

  private static final Logger LOGGER = Logger.getLogger(CLI.class.getName());
  private static final String WITH_VALUE = " with value ";

  /**
   * The main method of the CLI class. It parses the command line arguments and
   * triggers the
   *
   * @param args The command line arguments.
   */
  public static void main(String[] args) {
    new CLI().parseCLI(args);
  }

  static class MavenBuildException extends RuntimeException {
    public MavenBuildException(String message) {
      super(message);
    }
  }

  /**
   * Parses the command line arguments and triggers the generation of the
   * necessary files and the
   * build of the project.
   *
   * @param args The command line arguments.
   */
  public void parseCLI(String[] args) {
    Options options = new Options();
    options.addOption(new MetamodelOption());
    options.addOption(new FolderOption());
    options.addOption(new UserInteractorOption());
    options.addOption(new ReactionOption());
    CommandLineParser parser = new DefaultParser();
    VitruvConfiguration configuration = new VitruvConfiguration();

    try {
      CommandLine line = parser.parse(options, args);
      VirtualModelBuilder builder = new VirtualModelBuilder();
      for (Option option : line.getOptions()) {
        LOGGER.info(
            "Preparing option " + option.getLongOpt() + WITH_VALUE + option.getValuesList());
        ((VitruvCLIOption) option).prepare(line, configuration);
      }
      generateFiles(configuration);
      for (Option option : line.getOptions()) {
        LOGGER.info(
            "Preprocessing option "
                + option.getLongOpt()
                + WITH_VALUE
                + option.getValuesList());
        ((VitruvCLIOption) option).preBuild(line, builder, configuration);
      }
      ProcessBuilder pbuilder;
      String command = "mvn clean verify";
      if (System.getProperty("os.name").toLowerCase().contains("win")) {
        pbuilder = new ProcessBuilder("cmd.exe", "/c", command);
      } else {
        pbuilder = new ProcessBuilder("bash", "-c", command);
      }
      pbuilder.directory(
          new File(
              configuration
                  .getLocalPath()
                  .toFile()
                  .getAbsoluteFile()
                  .toString()
                  .replaceAll("\\s", "")));
      Process process = pbuilder.start();

      BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
      String oline;
      while ((oline = reader.readLine()) != null) {
        LOGGER.info(oline);
      }
      process.waitFor();
      if (process.exitValue() != 0) {
        throw new MavenBuildException(
            "Error occurred during Maven build! Please fix your setup accordingly! Exit code: "
                + process.exitValue());
      }
      for (Option option : line.getOptions()) {
        LOGGER.info(
            "Postprocessing option "
                + option.getLongOpt()
                + WITH_VALUE
                + option.getValuesList());
        ((VitruvCLIOption) option).postBuild(line, builder, configuration);
      }
      LOGGER.info(builder.buildAndInitialize().toString());
    } catch (ParseException exp) {
      LOGGER.info("Parsing failed.  Reason: " + exp.getMessage());
    } catch (IOException | InterruptedException e) {
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      LOGGER.info("Invoking maven to build the project failed.  Reason: " + e.getMessage());
    }
  }

  private void generateFiles(VitruvConfiguration configuration) throws IOException {
    GenerateFromTemplate generateFromTemplate = new GenerateFromTemplate();

    generateFromTemplate.generateRootPom(
        new File((configuration.getLocalPath() + "/pom.xml").replaceAll("\\s", "")),
        configuration.getPackageName());
    LOGGER.info("Generating root pom");

    generateFromTemplate.generateConsistencyPom(
        new File((configuration.getLocalPath() + "/consistency/pom.xml").replaceAll("\\s", "")),
        configuration.getPackageName());
    LOGGER.info("Generating consistency pom");

    generateFromTemplate.generateModelPom(
        new File((configuration.getLocalPath() + "/model/pom.xml").replaceAll("\\s", "")),
        configuration.getPackageName());
    LOGGER.info("Generating model pom");

    generateFromTemplate.generateVsumPom(
        new File((configuration.getLocalPath() + "/vsum/pom.xml").replaceAll("\\s", "")),
        configuration.getPackageName());
    LOGGER.info("Generating vsum pom");

    generateFromTemplate.generateP2WrappersPom(
        new File((configuration.getLocalPath() + "/p2wrappers/pom.xml").replaceAll("\\s", "")),
        configuration.getPackageName());
    LOGGER.info("Generating p2wrappers pom");

    generateFromTemplate.generateJavaUtilsPom(
        new File(
            (configuration.getLocalPath() + "/p2wrappers/javautils/pom.xml").replaceAll("\\s", "")),
        configuration.getPackageName());
    LOGGER.info("Generating p2wrappers javautils pom");

    generateFromTemplate.generateXAnnotationsPom(
        new File(
            (configuration.getLocalPath() + "/p2wrappers/activextendannotations/pom.xml")
                .replaceAll("\\s", "")),
        configuration.getPackageName());
    LOGGER.info("Generating p2wrappers xannotations pom");

    generateFromTemplate.generateEMFUtilsPom(
        new File(
            (configuration.getLocalPath() + "/p2wrappers/emfutils/pom.xml").replaceAll("\\s", "")),
        configuration.getPackageName());
    LOGGER.info("Generating p2wrappers emf utils pom");

    generateFromTemplate.generateVsumExample(
        new File(
            (configuration.getLocalPath() + "/vsum/src/main/java/VSUMExample.java")
                .replaceAll("\\s", "")),
        configuration.getPackageName());
    LOGGER.info("Generating vsum example java class");

    generateFromTemplate.generateVsumTest(
        new File(
            (configuration.getLocalPath() + "/vsum/src/test/java/VSUMExampleTest.java")
                .replaceAll("\\s", "")),
        configuration.getPackageName());
    LOGGER.info("Generating vsum example test java class");

    generateFromTemplate.generateProjectFile(
        new File((configuration.getLocalPath() + "/model/.project").replaceAll("\\s", "")),
        configuration.getPackageName());
    LOGGER.info("Generating project file");
    File workflow = new File(
        (configuration.getLocalPath() + "/model/workflow/generate.mwe2").replaceAll("\\s", ""));
    configuration.setWorkflow(workflow);

    generateFromTemplate.generateMwe2(
        workflow, configuration.getMetaModelLocations(), configuration);
    generateFromTemplate.generatePlugin(
        new File((configuration.getLocalPath() + "/model/plugin.xml").replaceAll("\\s", "")),
        configuration,
        configuration.getMetaModelLocations());
  }
}
