package tools.vitruv.cli.options;

import java.nio.file.Path;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

import tools.vitruv.cli.VirtualModelBuilderApplication;
import tools.vitruv.cli.configuration.VitruvConfiguration;
import tools.vitruv.framework.vsum.VirtualModelBuilder;

public abstract class VitruvCLIOption extends Option implements VirtualModelBuilderApplication {
  public VitruvCLIOption(String abbreviationName, String longName, boolean hasArguments, String description) {
    super(abbreviationName, longName, hasArguments, description);
  }

  public Path getPath(CommandLine cmd, VirtualModelBuilder builder) {
    return new FolderOption().getPath(cmd, builder);
  }

  @Override
  public final VirtualModelBuilder preBuild(CommandLine cmd, VirtualModelBuilder builder,
      VitruvConfiguration configuration) {
    if (!cmd.hasOption(getOpt())) {
      throw new IllegalArgumentException("Command called but not present!");
    }
    return applyInternal(cmd, builder, configuration);
  }

  @Override
  public VirtualModelBuilder postBuild(CommandLine cmd, VirtualModelBuilder builder,
      VitruvConfiguration configuration) {
    // the default operation is doing nothing after the maven build
    return builder;
  }

  public abstract VirtualModelBuilder applyInternal(CommandLine cmd, VirtualModelBuilder builder,
      VitruvConfiguration configuration);
}
