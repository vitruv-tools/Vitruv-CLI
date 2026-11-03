package tools.vitruv.cli;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.slf4j.LoggerFactory;

public class CLITest {

  @Test
  public void test() {
    CLI.main(
        new String[] {
          "-m",
          "src/test/resources/model/model.ecore,src/test/resources/model/model.genmodel;src/test/resources/model/model2.ecore,src/test/resources/model/model2.genmodel",
          "-f",
          "target/internal/",
          "-u",
          "default",
          "-r",
          "src/test/resources/consistency/templateReactions.reactions"
        });
  }

  @Test
  public void mainWithMultipleMetamodelsAndReactions() {
    CLI.main(
        new String[] {
          "-m",
          "src/test/resources/model/model.ecore,src/test/resources/model/model.genmodel;src/test/resources/model/model2.ecore,src/test/resources/model/model2.genmodel",
          "-f",
          "target/internal/",
          "-u",
          "default",
          "-rs",
          "src/test/resources/consistency"
        });
  }

  @Test
  public void mainWithRepeatedReactionOptions() {
    Logger cliLogger = (Logger) LoggerFactory.getLogger(CLI.class);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    cliLogger.addAppender(appender);

    try {
      CLI.main(
          new String[] {
            "-m",
            "src/test/resources/model/model.ecore,src/test/resources/model/model.genmodel;src/test/resources/model/model2.ecore,src/test/resources/model/model2.genmodel",
            "-f",
            "target/issue-15-multiple-reactions/",
            "-u",
            "default",
            "-r",
            "src/test/resources/consistency/templateReactions.reactions",
            "-r",
            "src/test/resources/consistency/templateReactions2.reactions"
          });
    } finally {
      cliLogger.detachAppender(appender);
      appender.stop();
    }

    assertThat(appender.list)
        .extracting(ILoggingEvent::getLevel)
        .doesNotContain(Level.ERROR);
  }

  /**
   * Builds a project with constraints. Needs the Vitruvius snapshots with the constraint SPI and
   * VitruvOCL, so it only runs with {@code -Dcli.e2e.constraints=true}.
   */
  @Test
  @EnabledIfSystemProperty(named = "cli.e2e.constraints", matches = "true")
  public void mainWithConstraints() throws Exception {
    Path project = Path.of("target/constraints/");
    CLI.main(
        new String[] {
          "-m",
          "src/test/resources/model/model.ecore,src/test/resources/model/model.genmodel;src/test/resources/model/model2.ecore,src/test/resources/model/model2.genmodel",
          "-f",
          project.toString(),
          "-u",
          "default",
          "-r",
          "src/test/resources/consistency/templateReactions.reactions",
          "-cs",
          "src/test/resources/constraints"
        });

    assertThat(project.resolve("vsum/src/main/resources/constraints/components.ocl")).exists();
    try (Stream<Path> files = Files.list(project.resolve("vsum/target"))) {
      Path jar =
          files
              .filter(file -> file.getFileName().toString().endsWith("-jar-with-dependencies.jar"))
              .findFirst()
              .orElseThrow();
      try (JarFile jarFile = new JarFile(jar.toFile())) {
        assertThat(jarFile.getEntry("constraints/components.ocl")).isNotNull();
        assertThat(
                jarFile.getEntry(
                    "tools/vitruv/dsls/vitruvocl/vitruv/VitruvOclConstraintChecker.class"))
            .isNotNull();
      }
    }
  }

  @Test
  public void test1() {
    CLI.main(
        new String[] {
          "-f", "target/internal/",
          "-u", "default",
          "-r", "src/test/resources/consistency/templateReactions.reactions"
        });
  }
}
