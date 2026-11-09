package tools.vitruv.cli.validate;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.PrintStream;

/** Prints the answer of the Vitruv server to a constraint validation in a readable form. */
class ValidationReport {
  private final PrintStream out;

  ValidationReport(PrintStream out) {
    this.out = out;
  }

  /**
   * Prints one block per constraint and a summary.
   *
   * @param response the answer of {@code POST /vsum/constraints/validate}
   * @return {@link ValidateCommand#SATISFIED} if all constraints are satisfied, otherwise {@link
   *     ValidateCommand#NOT_SATISFIED}
   */
  int print(JsonNode response) {
    int satisfied = 0;
    int violated = 0;
    int failed = 0;
    for (JsonNode result : response.path("results")) {
      String id = result.path("constraintId").asText();
      switch (result.path("status").asText()) {
        case "SATISFIED" -> {
          out.println("[PASS]  " + id);
          satisfied++;
        }
        case "VIOLATED" -> {
          printViolated(id, result);
          violated++;
        }
        default -> {
          out.println("[ERROR] " + id);
          for (JsonNode error : result.path("errors")) {
            out.println("        " + error.asText());
          }
          failed++;
        }
      }
    }
    out.println();
    out.println(
        String.format(
            "%d constraints validated: %d satisfied, %d violated, %d could not be checked",
            satisfied + violated + failed, satisfied, violated, failed));
    return violated == 0 && failed == 0
        ? ValidateCommand.SATISFIED
        : ValidateCommand.NOT_SATISFIED;
  }

  private void printViolated(String id, JsonNode result) {
    JsonNode violations = result.path("violations");
    String severity = result.path("severity").isTextual() ? result.get("severity").asText() : null;
    out.println(
        "[FAIL]  "
            + id
            + (severity == null ? "" : " (" + severity + ")")
            + ": "
            + violations.size()
            + (violations.size() == 1 ? " violation" : " violations"));
    for (JsonNode violation : violations) {
      StringBuilder line = new StringBuilder("        ").append(violation.path("uuid").asText());
      appendIfPresent(line, " ", violation, "eClass");
      appendIfPresent(line, " in ", violation, "resource");
      appendIfPresent(line, ": ", violation, "message");
      out.println(line);
    }
  }

  private static void appendIfPresent(
      StringBuilder line, String separator, JsonNode node, String field) {
    if (node.hasNonNull(field) && !node.get(field).asText().isBlank()) {
      line.append(separator).append(node.get(field).asText());
    }
  }
}
