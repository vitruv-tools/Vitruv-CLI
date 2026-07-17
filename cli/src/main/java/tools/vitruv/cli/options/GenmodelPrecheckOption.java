package tools.vitruv.cli.options;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import org.apache.commons.cli.CommandLine;
import lombok.extern.slf4j.Slf4j;
import tools.vitruv.cli.configuration.MetamodelLocation;
import tools.vitruv.cli.configuration.VitruvConfiguration;
import tools.vitruv.framework.vsum.VirtualModelBuilder;

/** CLI option to inspect and optionally standardize .genmodel files for MWE2 compatibility. */
@Slf4j
public class GenmodelPrecheckOption extends VitruvCLIOption {

  private static final String OPT = "pg";
  private static final String APPLY = "apply";

  /**
   * Prefix for the machine-readable status line consumers (e.g. the Methodologist backend, which
   * invokes this CLI as a subprocess and parses its stdout) look for to determine the outcome of a
   * precheck run. Must be followed by one of {@code CLEAN}, {@code ISSUES_FOUND}, or {@code
   * FIXES_APPLIED} on the same line.
   */
  private static final String GENMODEL_PRECHECK_STATUS_PREFIX = "GENMODEL_PRECHECK_STATUS:";

  /** Constructs the genmodel precheck option. */
  public GenmodelPrecheckOption() {
    super(
        OPT,
        "precheck-genmodel",
        false,
        "Precheck and standardize .genmodel files for MWE2 template compatibility.");
    this.setArgs(0);
  }

  /**
   * Runs the precheck flow.
   *
   * <p>Usage behaviour:
   *
   * <ul>
   *   <li>{@code -pg} → show problems and ask the user whether fixes should be applied
   *   <li>{@code -pg --apply} → apply fixes immediately
   *   <li>{@code -pg -f <folder>} → ask and then continue generation if accepted
   *   <li>{@code -pg --apply -f <folder>} → fix immediately and continue generation
   * </ul>
   *
   * @param cmd the command line
   * @param configuration the Vitruv configuration
   */
  @Override
  public void prepare(CommandLine cmd, VitruvConfiguration configuration) {
    if (!cmd.hasOption(OPT)) {
      return;
    }

    boolean applyImmediately = cmd.hasOption(APPLY);
    List<MetamodelLocation> locations = getValidatedLocations(configuration);
    GenmodelPrecheck precheck = new GenmodelPrecheck();

    List<String> failures = new ArrayList<>();
    List<GenmodelPrecheck.Issue> previewIssues =
        collectPreviewIssues(locations, precheck, failures);

    throwIfFailuresExist(failures);

    if (previewIssues.isEmpty()) {
      log.info("No problems found in the provided genmodel files.");
      printStatusMarker("CLEAN");
      return;
    }

    printPreviewIssues(previewIssues);
    if (!handleConfirmation(applyImmediately)) {
      printStatusMarker("ISSUES_FOUND");
      return;
    }

    List<GenmodelPrecheck.Issue> appliedIssues = applyFixes(locations, precheck);
    printAppliedIssues(appliedIssues);
    printStatusMarker("FIXES_APPLIED");
  }

  /**
   * Prints the machine-readable status marker directly to stdout.
   *
   * <p>Uses {@code System.out} rather than the SLF4J logger: consumers (see {@link
   * #GENMODEL_PRECHECK_STATUS_PREFIX}) match the marker at the start of a stdout line, but the
   * logger's pattern layout prefixes every line with a timestamp/level/logger name, which would
   * never satisfy that match.
   *
   * @param status one of {@code CLEAN}, {@code ISSUES_FOUND}, or {@code FIXES_APPLIED}
   */
  private void printStatusMarker(String status) {
    System.out.println(GENMODEL_PRECHECK_STATUS_PREFIX + " " + status);
  }

  /**
   * Returns the configured metamodel locations and validates that they exist.
   *
   * @param configuration the current CLI configuration
   * @return the configured metamodel locations
   */
  private List<MetamodelLocation> getValidatedLocations(VitruvConfiguration configuration) {
    List<MetamodelLocation> locations = configuration.getMetaModelLocations();
    if (locations == null || locations.isEmpty()) {
      throw new IllegalArgumentException(
          "No metamodels configured. Provide -m/--metamodel before running --precheck-genmodel.");
    }
    return locations;
  }

  /**
   * Collects preview issues for all configured genmodel files and records file-level failures.
   *
   * @param locations the metamodel locations
   * @param precheck the precheck service
   * @param failures the failure collector
   * @return the collected preview issues
   */
  private List<GenmodelPrecheck.Issue> collectPreviewIssues(
      List<MetamodelLocation> locations, GenmodelPrecheck precheck, List<String> failures) {

    List<GenmodelPrecheck.Issue> previewIssues = new ArrayList<>();

    for (MetamodelLocation loc : locations) {
      File genmodelFile = loc.genmodel();
      if (genmodelFile == null) {
        failures.add("Metamodel has no genmodel file reference: " + loc);
        continue;
      }
      if (!genmodelFile.exists()) {
        failures.add("Genmodel file does not exist: " + genmodelFile.getAbsolutePath());
        continue;
      }
      previewIssues.addAll(precheck.inspect(genmodelFile));
    }

    return previewIssues;
  }

  /**
   * Throws an exception if file-level failures were found.
   *
   * @param failures the collected failures
   */
  private void throwIfFailuresExist(List<String> failures) {
    if (!failures.isEmpty()) {
      throw new IllegalArgumentException(
          "Genmodel precheck failed:\n- " + String.join("\n- ", failures));
    }
  }

  /**
   * Stops processing early if no issues were found.
   *
   * @param previewIssues the detected preview issues
   */
  private void handleNoIssues(List<GenmodelPrecheck.Issue> previewIssues) {
    if (previewIssues.isEmpty()) {
      log.info("No problems found in the provided genmodel files.");
      throw new NoIssuesFoundException();
    }
  }

  /**
   * Prints the detected preview issues.
   *
   * @param previewIssues the detected issues
   */
  private void printPreviewIssues(List<GenmodelPrecheck.Issue> previewIssues) {
    log.info("We found some problems in your genmodel files:");
    for (GenmodelPrecheck.Issue issue : previewIssues) {
      log.info("- " + issue);
    }
  }

  /**
   * Handles interactive confirmation when automatic apply is not enabled.
   *
   * @param applyImmediately whether fixes should be applied immediately
   * @return {@code true} if fixes should be applied (either {@code applyImmediately} was set, or
   *     the user confirmed interactively), {@code false} if declined or no confirmation could be
   *     obtained (for example when running non-interactively with no stdin available)
   */
  private boolean handleConfirmation(boolean applyImmediately) {
    return applyImmediately || askForFixConfirmation();
  }

  /**
   * Applies fixes to all existing genmodel files.
   *
   * @param locations the metamodel locations
   * @param precheck the precheck service
   * @return the list of applied issues
   */
  private List<GenmodelPrecheck.Issue> applyFixes(
      List<MetamodelLocation> locations, GenmodelPrecheck precheck) {

    List<GenmodelPrecheck.Issue> appliedIssues = new ArrayList<>();

    for (MetamodelLocation loc : locations) {
      File genmodelFile = loc.genmodel();
      if (genmodelFile != null && genmodelFile.exists()) {
        appliedIssues.addAll(precheck.process(genmodelFile));
      }
    }

    return appliedIssues;
  }

  /**
   * Prints the applied changes summary.
   *
   * @param appliedIssues the applied issues
   */
  private void printAppliedIssues(List<GenmodelPrecheck.Issue> appliedIssues) {
    if (appliedIssues.isEmpty()) {
      log.info("No changes were necessary.");
      return;
    }

    log.info("Applied genmodel changes:");
    for (GenmodelPrecheck.Issue issue : appliedIssues) {
      log.info("- " + issue);
    }
  }

  /** Internal control-flow exception used to stop prepare processing when no issues are found. */
  private static final class NoIssuesFoundException extends RuntimeException {}

  /**
   * Prompts the user to confirm whether detected fixes should be applied.
   *
   * <p>When run non-interactively (no stdin available, e.g. invoked as a subprocess with its input
   * stream closed) there is no line to read; this is treated as a decline rather than a crash.
   *
   * @return {@code true} if the user confirmed the fixes
   */
  public boolean askForFixConfirmation() {
    log.info("Do you want to fix them? [y/N]: ");
    Scanner scanner = new Scanner(System.in);
    if (!scanner.hasNextLine()) {
      return false;
    }
    String input = scanner.nextLine();
    String normalized = input == null ? "" : input.trim().toLowerCase(Locale.ROOT);
    return "y".equals(normalized) || "yes".equals(normalized);
  }

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
