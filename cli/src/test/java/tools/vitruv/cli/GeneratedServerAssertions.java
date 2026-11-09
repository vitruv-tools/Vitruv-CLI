package tools.vitruv.cli;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;

/** Checks the executable artifact, including binary compatibility of its server dependencies. */
final class GeneratedServerAssertions {
  private GeneratedServerAssertions() {}

  static void assertStartsAndStops(Path project, String packageName, boolean constraints)
      throws Exception {
    Path jar;
    try (var files = Files.list(project.resolve("vsum/target"))) {
      jar = files.filter(path -> path.toString().endsWith("-jar-with-dependencies.jar"))
          .findFirst().orElseThrow().toAbsolutePath();
    }
    try (JarFile archive = new JarFile(jar.toFile())) {
      assertThat(archive.getManifest().getMainAttributes().getValue("Main-Class"))
          .isEqualTo(packageName + ".vsum.VSUMExample");
    }
    for (boolean eof : new boolean[] {true, false}) {
      int port;
      try (ServerSocket available = new ServerSocket(0)) {
        port = available.getLocalPort();
      }
      Path work = project.resolve("target/server-" + (eof ? "eof" : "quit")).toAbsolutePath();
      Files.createDirectories(work);
      Path log = work.resolve("server.log");
      String binary = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win")
          ? "java.exe" : "java";
      Process process = new ProcessBuilder(
          Path.of(System.getProperty("java.home"), "bin", binary).toString(),
          "-jar", jar.toString(), Integer.toString(port))
          .directory(work.toFile()).redirectErrorStream(true).redirectOutput(log.toFile()).start();
      try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build()) {
        if (eof) {
          process.getOutputStream().close();
        }
        URI base = URI.create("http://localhost:" + port);
        boolean ready = false;
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        while (process.isAlive() && System.nanoTime() < deadline) {
          try {
            client.send(HttpRequest.newBuilder(base).timeout(Duration.ofSeconds(1)).GET().build(),
                HttpResponse.BodyHandlers.discarding());
            ready = true;
            break;
          } catch (java.io.IOException ignored) {
            if (process.waitFor(100, TimeUnit.MILLISECONDS)) {
              break;
            }
          }
        }
        assertThat(ready).withFailMessage("Generated server did not start:%n%s", Files.readString(log))
            .isTrue();
        if (constraints) {
          var response = client.send(HttpRequest.newBuilder(base.resolve("/vsum/constraints/validate"))
              .timeout(Duration.ofSeconds(10)).header("Content-Type", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString("{\"includeModels\":false}")).build(),
              HttpResponse.BodyHandlers.ofString());
          assertThat(response.statusCode()).withFailMessage("Validation response: %s", response.body())
              .isEqualTo(200);
          var results = new ObjectMapper().readTree(response.body()).path("results");
          assertThat(results.isArray()).isTrue();
          assertThat(results.size()).isGreaterThan(0);
          for (var result : results) {
            assertThat(result.path("status").asText()).isEqualTo("SATISFIED");
          }
        }
        if (eof) {
          assertThat(process.waitFor(1, TimeUnit.SECONDS))
              .withFailMessage("Server exited on EOF:%n%s", Files.readString(log)).isFalse();
        } else {
          process.getOutputStream().write("quit\n".getBytes(StandardCharsets.UTF_8));
          process.getOutputStream().flush();
          assertThat(process.waitFor(15, TimeUnit.SECONDS)).as("Server exits on quit").isTrue();
          assertThat(process.exitValue()).withFailMessage("Server failed:%n%s", Files.readString(log))
              .isZero();
        }
      } finally {
        if (process.isAlive()) {
          process.destroy();
          if (!process.waitFor(10, TimeUnit.SECONDS)) {
            process.destroyForcibly().waitFor(10, TimeUnit.SECONDS);
          }
        }
        process.getOutputStream().close();
      }
    }
  }
}
