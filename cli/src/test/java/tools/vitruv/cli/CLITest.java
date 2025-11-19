package tools.vitruv.cli;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.commons.cli.MissingOptionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import tools.vitruv.cli.exceptions.MissingModelException;

public class CLITest {
  /**
   * Path to all V-SUMM elements.
   */
  private static final String RESOURCES = "src/test/resources";
  /**
   * Path to all metamodels.
   */
  private static final String MODELS = RESOURCES + "/model";
  /**
   * Path to all Reactions.
   */
  private static final String REACTIONS = RESOURCES + "/consistency";

  @Test
  @DisplayName("succeeds with creating a simple V-SUM")
  public void buildVSUM() {
    assertDoesNotThrow(() -> CLI.main(
        new String[] {
          "-m",
          MODELS + "model.ecore," + MODELS + "model.genmodel;"
              + MODELS + "model2.ecore," + MODELS + "model2.genmodel",
          "-f",
          "target/internal/",
          "-u",
          "default",
          "-r",
          REACTIONS + "templateReactions.reactions"
        })
    );
  }

  @Test
  @DisplayName("succeeds with using a folder for reactions")
  public void buildWithMultipleReactions() {
    assertDoesNotThrow(() -> CLI.main(
        new String[] {
          "-m",
          MODELS + "model.ecore," + MODELS + "model.genmodel;"
              + MODELS + "model2.ecore," + MODELS + "model2.genmodel",
          "-f",
          "target/internal/",
          "-u",
          "default",
          "-R",
          REACTIONS
        })
      );
  }

  @Test
  @DisplayName("fails when metamodels are omitted")
  public void failWithoutMetamodels() {
    assertThrows(MissingOptionException.class, () -> CLI.main(
        new String[] {
          "-f", "target/internal/",
          "-u", "default",
          "-r", REACTIONS + "templateReactions.reactions"
        })
    );
  }
}
