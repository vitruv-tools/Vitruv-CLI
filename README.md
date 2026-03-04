# Vitruv-CLI
This project contains the CLI for filling in the blanks of the methodologist's framework. 

Options:
- f (--folder): The path to the folder the Vitruv project should be instantiated in.
- m (--metamodel): A semicolon separated list of pairs of paths to the metamodels and their genmodels that are used in the reactions, e.g., MyMetamodel.ecore,MyGenmodel.genmodel;MyMetamodel1.ecore,MyGenmodel1.genmodel.
- r (--reation): The path to the file the Reactions are stored in.
- rs (--reactions): The path to a directory containing multiple .reactions files. All reaction files in this directory will be used during the build. Mutually exclusive with -r/--reaction.
- u (--userinteractor): Specify the path to a specific user interactor, use the keyword 'default' to denote that you want to use a default user interactor without functionality.
- pg (--precheck-genmodel): Precheck and standardize `.genmodel` files before running the CLI workflow.

## GenModel Precheck

The `-pg` (`--precheck-genmodel`) option validates and standardizes all provided `.genmodel` files to ensure compatibility with the Methodologist MWE2 generator workflow.

The following adjustments may be applied automatically:

- Remove unsupported attributes:
    - `complianceLevel`
    - `compliance`
    - `editDirectory`
    - `editorDirectory`
    - `testsDirectory`
    - `editPluginID`
    - `editorPluginID`
    - `testsPluginID`

- Ensure required conventions:
    - `basePackage` equals `modelPluginID`
    - `modelDirectory` equals  
      `/<modelPluginID>/target/generated-sources/ecore`
    - `creationIcons` is set to `false`
    - `foreignModel` exists (defaults to `<genmodel-name>.ecore` if missing)

### Two ways to use this option

**1. Standalone validation**

You can run the precheck to normalize `.genmodel` files before building.

vitruv-cli -pg -m MyMetamodel.ecore,MyGenmodel.genmodel

**2. As part of the normal CLI workflow**

The precheck can also run together with project generation.
vitruv-cli -pg -f projectFolder -m MyMetamodel.ecore,MyGenmodel.genmodel -r reactions.reactions

In this case the `.genmodel` files are validated and standardized before the build continues.

## Framework-internal Dependencies

This project depends on the following other projects from the Vitruvius framework:
- [Vitruv-Change](https://github.com/vitruv-tools/Vitruv-Change)
- [Vitruv](https://github.com/vitruv-tools/Vitruv)
