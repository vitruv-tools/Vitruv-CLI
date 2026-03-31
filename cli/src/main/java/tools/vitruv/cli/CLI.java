package tools.vitruv.cli;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import lombok.extern.slf4j.Slf4j;
import tools.vitruv.cli.options.ApplyOption;
import tools.vitruv.cli.configuration.VitruvConfiguration;
import tools.vitruv.cli.exceptions.MissingModelException;
import tools.vitruv.cli.options.FolderOption;
import tools.vitruv.cli.options.GenmodelPrecheckOption;
import tools.vitruv.cli.options.MetamodelOption;
import tools.vitruv.cli.options.ReactionOption;
import tools.vitruv.cli.options.ReactionsOption;
import tools.vitruv.cli.options.UserInteractorOption;
import tools.vitruv.cli.options.VitruvCLIOption;
import tools.vitruv.framework.vsum.VirtualModelBuilder;

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
    CommandLineParser parser = new DefaultParser();
    VitruvConfiguration configuration = new VitruvConfiguration();

    try {
      ParsedCli parsedCli = createParsedCli(parser, args);
      CommandLine line = parsedCli.line();

      validateReactionOptions(line);

      VirtualModelBuilder builder = new VirtualModelBuilder();

      prepareOptionsInOrder(line, configuration, parsedCli);

      if (shouldStopAfterPrecheck(line)) {
        return;
      }

      generateFiles(configuration);
      runPreBuild(line, builder, configuration);
      runMavenBuild(configuration);
      runPostBuild(line, builder, configuration);

      log.info(builder.buildAndInitialize().toString());
    } catch (ParseException exp) {
      log.error("Parsing failed.  Reason: " + exp.getMessage());
    } catch (IllegalArgumentException exp) {
      log.error("Invalid CLI argument or option.  Reason: " + exp.getMessage());
    } catch (IOException | InterruptedException e) {
      log.error("Invoking maven to build the project failed.  Reason: " + e.getMessage());
    } catch (MissingModelException e) {
      log.error("Generating files failed (missing models).  Reason: " + e.getMessage());
    }
  }

  /**
   * Parses CLI arguments and creates the option instances used by this invocation.
   *
   * @param parser the command line parser
   * @param args the raw CLI arguments
   * @return the parsed CLI data
   * @throws ParseException if parsing fails
   */
  private ParsedCli createParsedCli(CommandLineParser parser, String[] args) throws ParseException {
    boolean precheckRequested = hasArg(args, "-pg") || hasArg(args, "--precheck-genmodel");

    MetamodelOption metamodelOpt = new MetamodelOption();
    FolderOption folderOpt = new FolderOption();
    UserInteractorOption userOpt = new UserInteractorOption();
    ReactionOption reactionOpt = new ReactionOption();
    ReactionsOption reactionsOpt = new ReactionsOption();
    GenmodelPrecheckOption precheckOpt = new GenmodelPrecheckOption();
    ApplyOption applyOpt = new ApplyOption();

    if (precheckRequested) {
      folderOpt.setRequired(false);
      userOpt.setRequired(false);
      reactionOpt.setRequired(false);
      reactionsOpt.setRequired(false);
    }

    Options options = new Options();
    options.addOption(metamodelOpt);
    options.addOption(folderOpt);
    options.addOption(userOpt);
    options.addOption(reactionOpt);
    options.addOption(reactionsOpt);
    options.addOption(precheckOpt);
    options.addOption(applyOpt);

    CommandLine line = parser.parse(options, args);
    return new ParsedCli(
        line, metamodelOpt, folderOpt, userOpt, reactionOpt, reactionsOpt, precheckOpt, applyOpt);
  }

  /**
   * Validates mutually exclusive reaction options.
   *
   * @param line the parsed command line
   * @throws ParseException if both reaction options are provided
   */
  private void validateReactionOptions(CommandLine line) throws ParseException {
    if (line.hasOption("r") && line.hasOption("rs")) {
      throw new ParseException(
          "Options -r/--reaction and -rs/--reactions-source are mutually exclusive.");
    }
  }

  /**
   * Prepares CLI options in the required order.
   *
   * @param line the parsed command line
   * @param configuration the Vitruv configuration
   * @param parsedCli the parsed CLI wrapper
   */
  private void prepareOptionsInOrder(
      CommandLine line, VitruvConfiguration configuration, ParsedCli parsedCli) {

    prepareOptionIfPresent(line, "m", parsedCli.metamodelOpt(), configuration);
    prepareOptionIfPresent(line, "pg", parsedCli.precheckOpt(), configuration);
    prepareOptionIfPresent(line, "f", parsedCli.folderOpt(), configuration);
    prepareOptionIfPresent(line, "u", parsedCli.userOpt(), configuration);
    prepareOptionIfPresent(line, "r", parsedCli.reactionOpt(), configuration);
    prepareOptionIfPresent(line, "rs", parsedCli.reactionsOpt(), configuration);
  }

  /**
   * Prepares a single option if it is present on the command line.
   *
   * @param line the parsed command line
   * @param opt the short option name
   * @param option the CLI option instance
   * @param configuration the Vitruv configuration
   */
  private void prepareOptionIfPresent(
      CommandLine line, String opt, VitruvCLIOption option, VitruvConfiguration configuration) {
    if (!line.hasOption(opt)) {
      return;
    }

    log.info(
        "Preparing option " + option.getLongOpt() + " with value " + formatOptionValues(line, opt));
    option.prepare(line, configuration);
  }

  /**
   * Returns whether execution should stop after precheck.
   *
   * @param line the parsed command line
   * @return true if only precheck should run
   */
  private boolean shouldStopAfterPrecheck(CommandLine line) {
    return line.hasOption("pg") && !line.hasOption("f");
  }

  /**
   * Runs the preBuild phase for all present options.
   *
   * @param line the parsed command line
   * @param builder the virtual model builder
   * @param configuration the Vitruv configuration
   */
  private void runPreBuild(
      CommandLine line, VirtualModelBuilder builder, VitruvConfiguration configuration) {
    for (Option option : line.getOptions()) {
      log.info(
          "Preprocessing option " + option.getLongOpt() + " with value " + option.getValuesList());
      ((VitruvCLIOption) option).preBuild(line, builder, configuration);
    }
  }

  /**
   * Runs the postBuild phase for all present options.
   *
   * @param line the parsed command line
   * @param builder the virtual model builder
   * @param configuration the Vitruv configuration
   */
  private void runPostBuild(
      CommandLine line, VirtualModelBuilder builder, VitruvConfiguration configuration) {
    for (Option option : line.getOptions()) {
      log.info(
          "Postprocessing option " + option.getLongOpt() + " with value " + option.getValuesList());
      ((VitruvCLIOption) option).postBuild(line, builder, configuration);
    }
  }

  /**
   * Executes the Maven build in the generated project directory.
   *
   * @param configuration the Vitruv configuration
   * @throws IOException if the process cannot be started
   * @throws InterruptedException if the process is interrupted
   */
  private void runMavenBuild(VitruvConfiguration configuration)
      throws IOException, InterruptedException {
    ProcessBuilder pbuilder = createMavenProcessBuilder(configuration);
    Process process = pbuilder.start();

    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(process.getInputStream()))) {
      String line;
      while ((line = reader.readLine()) != null) {
        log.info(line);
      }
    }

    process.waitFor();
    if (process.exitValue() != 0) {
      throw new Error(
          "Error occurred during maven build! Please fix your setup accordingly! Exit code: "
              + process.exitValue());
    }
  }

  /**
   * Creates the ProcessBuilder for the Maven build command.
   *
   * @param configuration the Vitruv configuration
   * @return the configured ProcessBuilder
   */
  private ProcessBuilder createMavenProcessBuilder(VitruvConfiguration configuration) {
    String command = "mvn clean verify";
    ProcessBuilder pbuilder;

    if (System.getProperty("os.name").toLowerCase().contains("win")) {
      pbuilder = new ProcessBuilder("cmd.exe", "/c", command);
    } else {
      pbuilder = new ProcessBuilder("bash", "-c", command);
    }

    pbuilder.directory(
        new File(configuration.getLocalPath().toFile().getAbsoluteFile().toString().trim()));
    return pbuilder;
  }

  /**
   * Formats option values for logging.
   *
   * @param line the parsed command line
   * @param opt the short option name
   * @return the formatted option values
   */
  private String formatOptionValues(CommandLine line, String opt) {
    String[] values = line.getOptionValues(opt);
    return values == null ? "[]" : Arrays.toString(values);
  }

  /** Holds parsed command line data together with the created option instances. */
  private record ParsedCli(
      CommandLine line,
      MetamodelOption metamodelOpt,
      FolderOption folderOpt,
      UserInteractorOption userOpt,
      ReactionOption reactionOpt,
      ReactionsOption reactionsOpt,
      GenmodelPrecheckOption precheckOpt,
      ApplyOption applyOpt) {}

  private static boolean hasArg(String[] args, String needle) {
    for (String a : args) {
      if (needle.equals(a)) return true;
    }
    return false;
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
        configuration.getPackageName(),
        configuration.getModelNames());
    log.info("Generating vsum example java class");

    generateFromTemplate.generateVsumTest(
        new File(
            (configuration.getLocalPath() + "/vsum/src/test/java/VSUMExampleTest.java").trim()),
        configuration.getPackageName());
    log.info("Generating vsum example test java class");

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
