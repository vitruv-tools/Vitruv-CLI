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
import tools.vitruv.cli.exceptions.MissingModelException;
import tools.vitruv.cli.options.FolderOption;
import tools.vitruv.cli.options.MetamodelOption;
import tools.vitruv.cli.options.ReactionOption;
import tools.vitruv.cli.options.UserInteractorOption;
import tools.vitruv.cli.options.VitruvCLIOption;
import tools.vitruv.framework.vsum.VirtualModelBuilder;
import lombok.extern.slf4j.Slf4j;

/**
 * The CLI class is the main entry point for the command line interface of the Vitruv framework. It
 * parses the command line arguments and triggers the generation of the necessary files and the
 * build of the project.
 */
@Slf4j
public class CLI {

  /**
   * The main method of the CLI class. It parses the command line arguments and triggers the
   * generation of the necessary files and the build of the project.
   *
   * @param args The command line arguments.
   */
  public static void main(String[] args) {
    new CLI().parseCLI(args);
  }

  /**
   * Parses the command line arguments and triggers the generation of the necessary files and the
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
        log.info("Preparing option {} with value {}", option.getLongOpt(), option.getValuesList());
        ((VitruvCLIOption) option).prepare(line, configuration);
      }
      generateFiles(configuration);
      for (Option option : line.getOptions()) {
        log.info(
            "Postprocessing option {} with value {}", option.getLongOpt(), option.getValuesList());
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
          new File(configuration.getLocalPath().toFile().getAbsoluteFile().toString().trim()));
      Process process = pbuilder.start();

      BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
      String oline;
      while ((oline = reader.readLine()) != null) {
        log.info(oline);
      }
      process.waitFor();
      if (process.exitValue() != 0) {
        throw new Error(
            "Error occurred during maven build! Please fix your setup accordingly! Exit code: "
                + process.exitValue());
      }
      for (Option option : line.getOptions()) {
        log.info("Preparing option {} with value {}", option.getLongOpt(), option.getValuesList());
        ((VitruvCLIOption) option).postBuild(line, builder, configuration);
      }
      log.info(builder.buildAndInitialize().toString());
    } catch (ParseException exp) {
      log.error("Parsing failed.  Reason: {}", exp.getMessage());
    } catch (IOException | InterruptedException e) {
      log.error("Invoking maven to build the project failed.  Reason: {}", e.getMessage());
    } catch (MissingModelException e) {
      log.error("Generating files failed (missing models).  Reason: " + e.getMessage());
    }
  }

  private void generateFiles(VitruvConfiguration configuration)
      throws IOException, MissingModelException {

    GenerateFromTemplate generateFromTemplate = new GenerateFromTemplate();

    generateFromTemplate.generateRootPom(
        new File((configuration.getLocalPath() + "/pom.xml").trim()),
        configuration.getPackageName());
    log.info("Generating root pom");

    generateFromTemplate.generateConsistencyPom(
        new File((configuration.getLocalPath() + "/consistency/pom.xml").trim()),
        configuration.getPackageName());
    log.info("Generating consistency pom");

    generateFromTemplate.generateModelPom(
        new File((configuration.getLocalPath() + "/model/pom.xml").trim()),
        configuration.getPackageName());
    log.info("Generating model pom");

    generateFromTemplate.generateVsumPom(
        new File((configuration.getLocalPath() + "/vsum/pom.xml").trim()),
        configuration.getPackageName());
    log.info("Generating vsum pom");

    generateFromTemplate.generateP2WrappersPom(
        new File((configuration.getLocalPath() + "/p2wrappers/pom.xml").trim()),
        configuration.getPackageName());
    log.info("Generating p2wrappers pom");

    generateFromTemplate.generateJavaUtilsPom(
        new File((configuration.getLocalPath() + "/p2wrappers/javautils/pom.xml").trim()),
        configuration.getPackageName());
    log.info("Generating p2wrappers javautils pom");

    generateFromTemplate.generateXAnnotationsPom(
        new File(
            (configuration.getLocalPath() + "/p2wrappers/activextendannotations/pom.xml").trim()),
        configuration.getPackageName());
    log.info("Generating p2wrappers xannotations pom");

    generateFromTemplate.generateEMFUtilsPom(
        new File((configuration.getLocalPath() + "/p2wrappers/emfutils/pom.xml").trim()),
        configuration.getPackageName());
    log.info("Generating p2wrappers emf utils pom");

    generateFromTemplate.generateVsumExample(
        new File((configuration.getLocalPath() + "/vsum/src/main/java/VSUMExample.java").trim()),
        configuration.getPackageName());
    log.info("Generating vsum example java class");

    generateFromTemplate.generateVsumTest(
        new File(
            (configuration.getLocalPath() + "/vsum/src/test/java/VSUMExampleTest.java").trim()),
        configuration.getPackageName());
    log.info("Generating vsum example test java");

    generateFromTemplate.generateProjectFile(
        new File((configuration.getLocalPath() + "/model/.project").trim()),
        configuration.getPackageName());
    log.info("Generating project file");
    File workflow =
        new File((configuration.getLocalPath() + "/model/workflow/generate.mwe2").trim());
    configuration.setWorkflow(workflow);

    generateFromTemplate.generateMwe2(
        workflow, configuration.getMetaModelLocations(), configuration);
    generateFromTemplate.generatePlugin(
        new File((configuration.getLocalPath() + "/model/plugin.xml").trim()),
        configuration,
        configuration.getMetaModelLocations());
  }
}
