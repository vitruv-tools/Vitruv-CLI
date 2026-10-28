# Vitruv-CLI
This project contains the CLI for filling in the blanks of the methodologist's framework. 

Options:
- f (--folder): The path to the folder the Vitruv project should be instantiated in.
- m (--metamodel): A semicolon separated list of pairs of paths to the metamodels and their genmodels that are used in the reactions, e.g., MyMetamodel.ecore,MyGenmodel.genmodel;MyMetamodel1.ecore,MyGenmodel1.genmodel.
- r (--reation): The path to the file the Reactions are stored in.
- rs (--reactions): The path to a directory containing multiple .reactions files. All reaction files in this directory will be used during the build. Mutually exclusive with -r/--reaction.
- u (--userinteractor): Specify the path to a specific user interactor, use the keyword 'default' to denote that you want to use a default user interactor without functionality.
- pg (--precheck-genmodel): Precheck and standardize `.genmodel` files before running the CLI workflow.
- apply (--apply): Apply detected `.genmodel` fixes immediately without interactive confirmation. Only relevant together with `-pg` / `--precheck-genmodel`.
- c (--constraint): The path to an OCL constraint file (`.ocl`) that is built into the VSUM. Can be repeated.
- cs (--constraints): The path to a directory whose `.ocl` files are built into the VSUM. Mutually exclusive with -c/--constraint.

## Constraints

OCL constraints are built into the generated VSUM with `-c` or `-cs`:

```bash
vitruv-cli -f projectFolder -m MyMetamodel.ecore,MyGenmodel.genmodel -r reactions.reactions -cs constraints
```

The files are copied to `vsum/src/main/resources/constraints/` under their own names (files with the same name get a numeric suffix), so they are part of the VSUM jar. `VSUMExample` registers them with the `VitruvOclConstraintChecker` of VitruvOCL. Every context must name its metamodel, e.g. `context model::Component inv named: self.name.size() > 0`.

Projects with constraints use the Vitruvius versions that provide the constraint SPI (framework 4.0.2-SNAPSHOT, VitruvOCL 0.1.0-SNAPSHOT), resolved from the Maven Central snapshots. Projects without constraints are generated as before.

### Validating the constraints of a running VSUM

The `validate` subcommand validates the constraints of a running VSUM through the REST API of its Vitruv server and lists the violating elements by their UUIDs:

```bash
vitruv-cli validate --url http://localhost:8080
vitruv-cli validate --url http://localhost:8080 --id model::Component::named --id model::Link::valid
```

- `-u` / `--url`: URL of the Vitruv server (default `http://localhost:8080`)
- `-i` / `--id`: id of a constraint to validate, can be repeated (default: all constraints)

```
[FAIL]  model::Component::named (CRITICAL): 1 violation
        3f1c…-uuid model::Component in /model/a.model: Component has no name
[PASS]  model::Link::valid

2 constraints validated: 1 satisfied, 1 violated, 0 could not be checked
```

The exit code is `0` if all validated constraints are satisfied, `1` if a constraint is violated or could not be checked, and `2` if the arguments are invalid or the server could not be asked.

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

### Confirmation and automatic apply

When `-pg` is used by itself, the CLI reports the detected `.genmodel` issues and asks whether they should be fixed.

```bash
vitruv-cli -pg -m MyMetamodel.ecore,MyGenmodel.genmodel
```
If you want the fixes to be applied immediately without confirmation, use --apply.

```bash
vitruv-cli -pg --apply -m MyMetamodel.ecore,MyGenmodel.genmodel
```
If -pg is combined with the normal generation workflow, --apply can also be used to fix .genmodel files first and then continue directly with generation.
```bash
vitruv-cli -pg --apply -f projectFolder -m MyMetamodel.ecore,MyGenmodel.genmodel -r reactions.reactions
```

If issues are found and fixes are declined, the CLI stops execution and does not continue with generation or build.

### Two ways to use this option

**1. Standalone validation**

You can run the precheck to normalize `.genmodel` files before building.
```bash
vitruv-cli -pg -m MyMetamodel.ecore,MyGenmodel.genmodel
```
**2. As part of the normal CLI workflow**

The precheck can also run together with project generation.
```bash
vitruv-cli -pg -f projectFolder -m MyMetamodel.ecore,MyGenmodel.genmodel -r reactions.reactions
```
In this case the `.genmodel` files are validated and standardized before the build continues.

## Framework-internal Dependencies

This project depends on the following other projects from the Vitruvius framework:
- [Vitruv-Change](https://github.com/vitruv-tools/Vitruv-Change)
- [Vitruv](https://github.com/vitruv-tools/Vitruv)
