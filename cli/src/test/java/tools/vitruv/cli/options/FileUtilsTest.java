package tools.vitruv.cli.options;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileUtilsTest {
  @TempDir Path tempDir;

  @Test
  void copyFile_shouldCopyIntoSubfolderOfAbsoluteFolder() throws Exception {
    Path source = Files.writeString(tempDir.resolve("source.txt"), "content");
    Path project = tempDir.resolve("project");

    File target = FileUtils.copyFile(source.toString(), project, "/sub/folder/");

    assertThat(target.toPath()).isEqualTo(project.resolve("sub/folder/source.txt"));
    assertThat(target).hasContent("content");
  }

  @Test
  void copyFile_shouldOverwriteExistingTarget() throws Exception {
    Path source = Files.writeString(tempDir.resolve("source.txt"), "new");
    Path project = tempDir.resolve("project");
    Files.createDirectories(project.resolve("sub"));
    Files.writeString(project.resolve("sub/source.txt"), "old");

    File target = FileUtils.copyFile(source.toString(), project, "/sub/");

    assertThat(target).hasContent("new");
  }

  @Test
  void copyFile_shouldResolveRelativePathsAgainstWorkingDirectory() {
    Path relativeProject = Path.of("target/file-utils-test");

    File target =
        FileUtils.copyFile("src/test/resources/model/model.ecore", relativeProject, "/copy/");

    assertThat(target.toPath())
        .isEqualTo(relativeProject.toAbsolutePath().resolve("copy/model.ecore"));
    assertThat(target).exists();
  }

  @Test
  void createFile_shouldCreateMissingParentDirectories() {
    Path file = tempDir.resolve("a/b/c.txt");

    FileUtils.createFile(file.toString());
    FileUtils.createFile(file.toString());

    assertThat(file).isEmptyFile();
  }

  @Test
  void createNewFolder_shouldReturnCreatedFolder() {
    Path folder = FileUtils.createNewFolder(tempDir, "new/folder");

    assertThat(folder).isEqualTo(tempDir.resolve("new/folder")).isDirectory();
    assertThat(FileUtils.createNewFolder(tempDir, "new/folder")).isDirectory();
  }

  @Test
  void findOption_shouldReturnTrimmedValueOfFirstMatch() throws Exception {
    Path file =
        Files.writeString(
            tempDir.resolve("x.reactions"), "import a\nreactions:  first \nreactions: second\n");

    assertThat(FileUtils.findOption(file.toFile(), "reactions:")).isEqualTo("first");
    assertThat(FileUtils.findOptions(file.toFile(), "reactions:"))
        .containsExactly("first", "second");
  }

  @Test
  void findOption_shouldFail_whenOptionIsMissing() throws Exception {
    Path file = Files.writeString(tempDir.resolve("x.reactions"), "import a\n");

    assertThatThrownBy(() -> FileUtils.findOption(file.toFile(), "reactions:"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(FileUtils.findOptions(file.toFile(), "reactions:")).isEmpty();
  }

  @Test
  void findOptions_shouldFail_whenFileIsMissing() {
    File missing = tempDir.resolve("missing.reactions").toFile();

    assertThatThrownBy(() -> FileUtils.findOptions(missing, "reactions:"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("missing.reactions");
  }
}
