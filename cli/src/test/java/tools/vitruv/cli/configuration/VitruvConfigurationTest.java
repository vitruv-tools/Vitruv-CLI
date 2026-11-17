package tools.vitruv.cli.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class VitruvConfigurationTest {
  private static final String MODEL_DIRECTORY = "src/test/resources/model/";

  @Test
  void setMetaModelLocations_shouldDeriveNamesAndPackageFromModels() {
    VitruvConfiguration configuration = new VitruvConfiguration();

    configuration.setMetaModelLocations(
        MODEL_DIRECTORY
            + "model.ecore,"
            + MODEL_DIRECTORY
            + "model.genmodel;"
            + MODEL_DIRECTORY
            + "model2.ecore,"
            + MODEL_DIRECTORY
            + "model2.genmodel");

    assertThat(configuration.getModelNames()).containsExactly("model", "model2");
    assertThat(configuration.getPackageName()).isEqualTo("tools.vitruv.methodologisttemplate");
    assertThat(configuration.getMetaModelLocations())
        .extracting(location -> location.metamodel().getName())
        .containsExactly("model.ecore", "model2.ecore");
    assertThat(configuration.getMetaModelLocations())
        .extracting(MetamodelLocation::modelDirectory)
        .containsOnly("/tools.vitruv.methodologisttemplate.model/target/generated-sources/ecore");
  }

  @Test
  void removeLastSegment_shouldDropEverythingAfterLastDot() {
    assertThat(VitruvConfiguration.removeLastSegment("a.b.c")).isEqualTo("a.b");
    assertThat(VitruvConfiguration.removeLastSegment("nodots")).isEqualTo("nodots");
  }

  @Test
  void setReactionLocations_shouldReplacePreviousLocations(@TempDir Path tempDir) {
    VitruvConfiguration configuration = new VitruvConfiguration();
    configuration.setReactionLocations(List.of(tempDir.resolve("a.reactions")));

    configuration.setReactionLocations(List.of(tempDir.resolve("b.reactions")));
    assertThat(configuration.getReactionLocations())
        .containsExactly(tempDir.resolve("b.reactions"));

    configuration.setReactionLocations(null);
    assertThat(configuration.getReactionLocations()).isEmpty();
  }

  @Test
  void getChangePropagationSpecificationNames_shouldBeEmptyWithoutReactions() {
    assertThat(new VitruvConfiguration().getChangePropagationSpecificationNames()).isEmpty();
  }

  @Test
  void getChangePropagationSpecificationNames_shouldContainAllSegmentsOfAllFiles(
      @TempDir Path tempDir) throws Exception {
    Path first =
        Files.writeString(
            tempDir.resolve("first.reactions"),
            "reactions: aToB\nin reaction to changes in a\n\nreactions:   bToA\n");
    Path second = Files.writeString(tempDir.resolve("second.reactions"), "reactions: other\n");
    VitruvConfiguration configuration = new VitruvConfiguration();
    configuration.setReactionLocations(List.of(first, second));

    assertThat(configuration.getChangePropagationSpecificationNames())
        .containsExactly(
            "mir.reactions.aToB.AToBChangePropagationSpecification",
            "mir.reactions.bToA.BToAChangePropagationSpecification",
            "mir.reactions.other.OtherChangePropagationSpecification");
  }
}
