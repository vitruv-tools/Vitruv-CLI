package tools.vitruv.cli;

import org.apache.commons.cli.CommandLine;

import tools.vitruv.cli.configuration.VitruvConfiguration;
import tools.vitruv.framework.vsum.VirtualModelBuilder;

public interface VirtualModelBuilderApplication {
  void prepare(CommandLine cmd, VitruvConfiguration configuration);

  VirtualModelBuilder preBuild(CommandLine cmd, VirtualModelBuilder builder, VitruvConfiguration configuration);

  VirtualModelBuilder postBuild(CommandLine cmd, VirtualModelBuilder builder, VitruvConfiguration configuration);
}