package tools.vitruv.cli;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.Test;

class CLITest {

  @Test
  void test() {
    String outputDirPath = "target/internal/";
    CLI.main(
        new String[] {
            "-m",
            "src/test/resources/model/model.ecore,src/test/resources/model/model.genmodel;src/test/resources/model/model2.ecore,src/test/resources/model/model2.genmodel",
            "-f", "target/internal/",
            "-u", "default",
            "-r", "src/test/resources/consistency/templateReactions.reactions"
        });
      File outputDir = new File(outputDirPath);
      assertTrue(outputDir.exists() && outputDir.isDirectory(),
            "Expected output directory '" + outputDirPath + "' to be created by CLI.");F
    }
}
