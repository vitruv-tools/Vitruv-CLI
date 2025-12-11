package tools.vitruv.cli;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.cli.MissingOptionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class CLITest {
  /**
   * Path to the built V-SUM.
   */
  private static final String TARGET_INTERNAL_PATH = "target/internal/";
  /**
   * Path to all V-SUMM elements.
   */
  private static final String RESOURCES = "src/test/resources/";
  /**
   * Path to all metamodels.
   */
  private static final String MODELS = RESOURCES + "model/";
  /**
   * Path to all Reactions.
   */
  private static final String REACTIONS = RESOURCES + "consistency/";

  @AfterEach
  void deleteVSUMDirectory() throws IOException {
    System.out.println("Clearing directory " + TARGET_INTERNAL_PATH);
    deleteFrom(new File(TARGET_INTERNAL_PATH));
  }

  void deleteFrom(File file) {
    if (file.isDirectory()) {
      var contents = file.listFiles();
      for (var content: contents) {
        deleteFrom(content);
      }
    }
    file.delete();
  }

  @Test
  @DisplayName("succeeds with creating a simple V-SUM")
  public void buildVSUM() {
    assertDoesNotThrow(() -> CLI.main(
        new String[] {
          "-m",
          MODELS + "model.ecore," + MODELS + "model.genmodel;"
              + MODELS + "model2.ecore," + MODELS + "model2.genmodel",
          "-f",
          TARGET_INTERNAL_PATH,
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
          TARGET_INTERNAL_PATH,
          "-u",
          "default",
          "-rs",
          REACTIONS
        })
    );
  }

  @Test
  @DisplayName("fails when metamodels are omitted")
  public void failWithoutMetamodels() {
    assertThrows(MissingOptionException.class, () -> CLI.main(
        new String[] {
          "-f", TARGET_INTERNAL_PATH,
          "-u", "default",
          "-r", REACTIONS + "templateReactions.reactions"
        })
    );
  }
}
