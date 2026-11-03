package tools.vitruv.cli.validate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * The {@code validate} subcommand: validates the constraints of a running VSUM through the REST API
 * of its Vitruv server and reports the violating elements by their UUIDs.
 */
public class ValidateCommand {
  /** Name of the subcommand on the command line. */
  public static final String NAME = "validate";

  /** Exit code if all validated constraints are satisfied. */
  public static final int SATISFIED = 0;

  /** Exit code if a constraint is violated or could not be checked. */
  public static final int NOT_SATISFIED = 1;

  /** Exit code if the arguments are invalid or the server could not be asked. */
  public static final int FAILED = 2;

  static final String DEFAULT_URL = "http://localhost:8080";
  private static final String VALIDATE_PATH = "/vsum/constraints/validate";
  private static final Duration TIMEOUT = Duration.ofMinutes(5);

  private final PrintStream out;
  private final PrintStream err;
  private final HttpClient httpClient;
  private final ObjectMapper mapper = new ObjectMapper();

  /**
   * Creates the command.
   *
   * @param out the stream the report is printed to
   * @param err the stream errors are printed to
   */
  public ValidateCommand(PrintStream out, PrintStream err) {
    this(out, err, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
  }

  ValidateCommand(PrintStream out, PrintStream err, HttpClient httpClient) {
    this.out = out;
    this.err = err;
    this.httpClient = httpClient;
  }

  /**
   * Runs the command.
   *
   * @param args the arguments after the subcommand name
   * @return {@link #SATISFIED}, {@link #NOT_SATISFIED} or {@link #FAILED}
   */
  public int run(String[] args) {
    Options options = options();
    CommandLine line;
    try {
      line = new DefaultParser().parse(options, args);
    } catch (ParseException e) {
      err.println("Invalid arguments: " + e.getMessage());
      printUsage(options);
      return FAILED;
    }
    if (line.hasOption("h")) {
      printUsage(options);
      return SATISFIED;
    }
    try {
      URI endpoint = endpoint(line.getOptionValue("url", DEFAULT_URL));
      String[] ids = line.getOptionValues("id");
      JsonNode response = validate(endpoint, ids == null ? null : List.of(ids));
      return new ValidationReport(out).print(response);
    } catch (IllegalArgumentException | ValidationRequestException e) {
      err.println(e.getMessage());
      return FAILED;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      err.println("Validation was interrupted");
      return FAILED;
    }
  }

  private Options options() {
    Options options = new Options();
    options.addOption(
        Option.builder("u")
            .longOpt("url")
            .hasArg()
            .argName("url")
            .desc("URL of the Vitruv server of the running VSUM (default " + DEFAULT_URL + ")")
            .build());
    options.addOption(
        Option.builder("i")
            .longOpt("id")
            .hasArg()
            .argName("constraintId")
            .desc("Id of a constraint to validate; can be repeated (default: all constraints)")
            .build());
    options.addOption(Option.builder("h").longOpt("help").desc("Prints this help").build());
    return options;
  }

  private void printUsage(Options options) {
    new HelpFormatter()
        .printHelp(
            new PrintWriter(out, true),
            HelpFormatter.DEFAULT_WIDTH,
            "vitruv-cli " + NAME + " [-u <url>] [-i <constraintId>]...",
            "Validates the constraints of a running VSUM.",
            options,
            HelpFormatter.DEFAULT_LEFT_PAD,
            HelpFormatter.DEFAULT_DESC_PAD,
            "Exit code: 0 all satisfied, 1 violated or not checkable, 2 failed.");
  }

  static URI endpoint(String url) {
    String trimmed = url.trim();
    while (trimmed.endsWith("/")) {
      trimmed = trimmed.substring(0, trimmed.length() - 1);
    }
    try {
      URI base = new URI(trimmed);
      boolean http =
          "http".equalsIgnoreCase(base.getScheme()) || "https".equalsIgnoreCase(base.getScheme());
      if (!http || base.getHost() == null) {
        throw new IllegalArgumentException("Not an http(s) URL of a Vitruv server: " + url);
      }
      return new URI(trimmed + VALIDATE_PATH);
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("Not an http(s) URL of a Vitruv server: " + url, e);
    }
  }

  private JsonNode validate(URI endpoint, List<String> constraintIds)
      throws InterruptedException {
    ObjectNode body = mapper.createObjectNode();
    if (constraintIds != null) {
      constraintIds.forEach(body.putArray("constraintIds")::add);
    }
    body.put("includeModels", false);
    HttpRequest request =
        HttpRequest.newBuilder(endpoint)
            .timeout(TIMEOUT)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .build();
    HttpResponse<String> response;
    try {
      response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    } catch (IOException e) {
      throw new ValidationRequestException(
          "Could not reach the Vitruv server at " + endpoint + ": " + e.getMessage(), e);
    }
    JsonNode json = parse(response.body());
    if (response.statusCode() != 200) {
      String error = json != null && json.hasNonNull("error") ? json.get("error").asText() : null;
      if (error == null && response.statusCode() == 404 && constraintIds != null) {
        error = "unknown constraint id among " + constraintIds;
      }
      throw new ValidationRequestException(
          "The Vitruv server answered "
              + response.statusCode()
              + (error == null ? "" : ": " + error));
    }
    if (json == null || !json.has("results")) {
      throw new ValidationRequestException(
          "The Vitruv server sent no validation result; is " + endpoint + " a Vitruv server?");
    }
    return json;
  }

  private JsonNode parse(String body) {
    if (body == null || body.isBlank()) {
      return null;
    }
    try {
      return mapper.readTree(body);
    } catch (IOException e) {
      return null;
    }
  }

  /** The validation request failed or its answer is unusable. */
  static class ValidationRequestException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    ValidationRequestException(String message) {
      super(message);
    }

    ValidationRequestException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
