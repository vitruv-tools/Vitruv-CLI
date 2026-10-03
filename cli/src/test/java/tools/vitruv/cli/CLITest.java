package tools.vitruv.cli;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
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
