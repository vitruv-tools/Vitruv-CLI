package tools.vitruv.cli.configuration;

import java.io.File;

public record MetamodelLocation(File metamodel, File genmodel, String genmodelUri) {
}
