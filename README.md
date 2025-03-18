# Vitruv-CLI
This project contains the CLI for filling in the blanks of the methodologist's framework. 

Options:
- f (--folder): The path to the folder the Vitruv project should be instantiated in.
- m (--metamodel): A semicolon separated list of pairs of paths to the metamodels and their genmodels that are used in the reactions, e.g., MyMetamodel.ecore,MyGenmodel.genmodel;MyMetamodel1.ecore,MyGenmodel1.genmodel.
- r (--reation): The path to the file the Reactions are stored in.
- u (--userinteractor): Specify the path to a specific user interactor, use the keyword 'default' to denote that you want to use a default user interactor without functionality.


## Framework-internal Dependencies

This project depends on the following other projects from the Vitruvius framework:
- [Vitruv-Change](https://github.com/vitruv-tools/Vitruv-Change)
- [Vitruv](https://github.com/vitruv-tools/Vitruv)
