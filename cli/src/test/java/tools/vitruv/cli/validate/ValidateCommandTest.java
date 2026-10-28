package tools.vitruv.cli.validate;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ValidateCommandTest {

  private static final String VIOLATED =
      """
      {"satisfied": false, "results": [
        {"constraintId": "model::Component::named", "name": "named", "status": "VIOLATED",
         "severity": "CRITICAL", "errors": [],
         "violations": [{"uuid": "abc-123", "eClass": "model::Component",
           "resource": "/model/a.model", "severity": "CRITICAL", "message": "no name"}]},
        {"constraintId": "model::Link::valid", "name": "valid", "status": "SATISFIED",
         "severity": null, "violations": [], "errors": []},
        {"constraintId": "model::Link::broken", "name": "broken", "status": "ERROR",
         "severity": null, "violations": [], "errors": ["Unknown type Foo"]}
      ]}
      """;

  private HttpServer server;
  private final AtomicReference<String> requestBody = new AtomicReference<>();
  private final AtomicReference<String> requestPath = new AtomicReference<>();
  private int status;
  private String response;

  private final ByteArrayOutputStream out = new ByteArrayOutputStream();
  private final ByteArrayOutputStream err = new ByteArrayOutputStream();
  private final ValidateCommand command =
      new ValidateCommand(
          new PrintStream(out, true, StandardCharsets.UTF_8),
          new PrintStream(err, true, StandardCharsets.UTF_8));

  @BeforeEach
  void startServer() throws IOException {
    server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          requestPath.set(exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath());
          requestBody.set(
              new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(status, bytes.length);
          exchange.getResponseBody().write(bytes);
          exchange.close();
        });
    server.start();
  }

  @AfterEach
  void stopServer() {
    server.stop(0);
  }

  private String url() {
    return "http://localhost:" + server.getAddress().getPort() + "/";
  }

  private String output() {
    return out.toString(StandardCharsets.UTF_8);
  }

  @Test
  void reportsViolationsWithTheUuidsOfTheViolatingElements() {
    status = 200;
    response = VIOLATED;

    int exitCode = command.run(new String[] {"--url", url()});

    assertThat(exitCode).isEqualTo(ValidateCommand.NOT_SATISFIED);
    assertThat(requestPath.get()).isEqualTo("POST /vsum/constraints/validate");
    assertThat(requestBody.get()).isEqualTo("{\"includeModels\":false}");
    assertThat(output())
        .contains("[FAIL]  model::Component::named (CRITICAL): 1 violation")
        .contains("abc-123 model::Component in /model/a.model: no name")
        .contains("[PASS]  model::Link::valid")
        .contains("[ERROR] model::Link::broken", "Unknown type Foo")
        .contains("3 constraints validated: 1 satisfied, 1 violated, 1 could not be checked");
  }

  @Test
  void validatesOnlyTheGivenConstraints() {
    status = 200;
    response =
        """
        {"satisfied": true, "results": [{"constraintId": "model::Link::valid", "status": "SATISFIED",
         "violations": [], "errors": []}]}
        """;

    int exitCode = command.run(new String[] {"-u", url(), "-i", "model::Link::valid", "-i", "x"});

    assertThat(exitCode).isEqualTo(ValidateCommand.SATISFIED);
    assertThat(requestBody.get())
        .isEqualTo("{\"constraintIds\":[\"model::Link::valid\",\"x\"],\"includeModels\":false}");
    assertThat(output()).contains("1 constraints validated: 1 satisfied");
  }

  @Test
  void reportsTheErrorOfTheServer() {
    status = 404;
    response = "{\"error\": \"Unknown constraint x\", \"diagnostics\": []}";

    int exitCode = command.run(new String[] {"-u", url(), "-i", "x"});

    assertThat(exitCode).isEqualTo(ValidateCommand.FAILED);
    assertThat(err.toString(StandardCharsets.UTF_8))
        .contains("answered 404: Unknown constraint x");
  }

  @Test
  void rejectsAnswersThatAreNoValidationResult() {
    status = 200;
    response = "<html>not a vitruv server</html>";

    assertThat(command.run(new String[] {"-u", url()})).isEqualTo(ValidateCommand.FAILED);
    assertThat(err.toString(StandardCharsets.UTF_8)).contains("sent no validation result");
  }

  @Test
  void failsWhenTheServerIsNotReachable() {
    String unreachable = url();
    server.stop(0);

    assertThat(command.run(new String[] {"-u", unreachable})).isEqualTo(ValidateCommand.FAILED);
    assertThat(err.toString(StandardCharsets.UTF_8)).contains("Could not reach the Vitruv server");
  }

  @Test
  void rejectsUrlsThatAreNoHttpUrls() {
    assertThat(command.run(new String[] {"-u", "ftp://localhost"}))
        .isEqualTo(ValidateCommand.FAILED);
    assertThat(err.toString(StandardCharsets.UTF_8)).contains("Not an http(s) URL");
  }

  @Test
  void rejectsUnknownArguments() {
    assertThat(command.run(new String[] {"--bogus"})).isEqualTo(ValidateCommand.FAILED);
    assertThat(output()).contains("usage: vitruv-cli validate");
  }

  @Test
  void printsTheHelp() {
    assertThat(command.run(new String[] {"--help"})).isEqualTo(ValidateCommand.SATISFIED);
    assertThat(output()).contains("--url", "--id", "Exit code");
  }

  @Test
  void usesLocalhostByDefault() {
    assertThat(ValidateCommand.endpoint(ValidateCommand.DEFAULT_URL))
        .hasToString("http://localhost:8080/vsum/constraints/validate");
  }
}
