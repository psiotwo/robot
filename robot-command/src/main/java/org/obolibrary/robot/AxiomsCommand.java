package org.obolibrary.robot;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.semanticweb.owlapi.model.*;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Manipulate selected axioms from an ontology.
 *
 * @author <a href="mailto:rctauber@gmail.com">Becky Tauber</a>
 */
public class AxiomsCommand implements Command {

  /**
   * Store the command-line options for the command.
   */
  private Options options;

  /**
   * Initialze the command.
   */
  public AxiomsCommand() {
    Options o = CommandLineHelper.getCommonOptions();
    o.addOption("i", "input", true, "load ontology from a file");
    o.addOption("I", "input-iri", true, "load ontology from an IRI");
    o.addOption("o", "output", true, "save ontology to a file");
    o.addOption(null, "base-iri", true, "specify a base namespace");
    o.addOption("s", "select", true, "select a set of terms based on relations");
    o.addOption("d", "operation", true, "'remove' or 'annotate'");
    o.addOption("a", "annotation", true, "annotations to create");
    options = o;
  }

  /**
   * Name of the command.
   *
   * @return name
   */
  public String getName() {
    return "axioms";
  }

  /**
   * Brief description of the command.
   *
   * @return description
   */
  public String getDescription() {
    return "manipulates axioms from an ontology";
  }

  /**
   * Command-line usage for the command.
   *
   * @return usage
   */
  public String getUsage() {
    return "robot " + getName() + " --input <file> " + "--output <file>";
  }

  /**
   * Command-line options for the command.
   *
   * @return options
   */
  public Options getOptions() {
    return options;
  }

  /**
   * Handle the command-line and file operations.
   *
   * @param args strings to use as arguments
   */
  public void main(String[] args) {
    try {
      execute(null, args);
    } catch (Exception e) {
      CommandLineHelper.handleException(getUsage(), getOptions(), e);
    }
  }

  /**
   * Given an input state and command line arguments, create a new ontology with removed/deprecated axioms and
   * return a new state. The input ontology is not changed.
   *
   * @param state the state from the previous command, or null
   * @param args  the command-line arguments
   * @return a new state with the new ontology
   * @throws Exception on any problem
   */
  public CommandState execute(CommandState state, final String[] args) throws Exception {
    final CommandLine line = CommandLineHelper.getCommandLine(getUsage(), getOptions(), args);
    if (line == null) {
      return null;
    }

    final IOHelper ioHelper = CommandLineHelper.getIOHelper(line);
    state = CommandLineHelper.updateInputOntology(ioHelper, state, line);
    final OWLOntology ontology = state.getOntology();
    final OWLOntologyManager manager = ontology.getOWLOntologyManager();

    final String select = CommandLineHelper.getOptionalValue(line, "select");
    final String operation = CommandLineHelper.getOptionalValue(line, "operation");
    Objects.requireNonNull(operation, "One of 'remove' or 'deprecate' must be selected.");

    final Set<OWLAxiom> axioms = select == null ?
      ontology.getAxioms() : RelatedObjectsHelper.selectAxiomsByPattern(ontology, ioHelper, select);

    final Set<OWLAxiom> axiomsWithoutAnnotations = axioms.stream().map( a -> a.getAxiomWithoutAnnotations() ).collect(Collectors.toSet());
    axioms.addAll(axiomsWithoutAnnotations);

    if (operation.equals("remove")) {
      manager.removeAxioms(ontology, axioms);
    } else if (operation.equals("annotate")) {
      final String annotationString = CommandLineHelper.getOptionalValue(line, "annotation");
      final Set<OWLAnnotation> annotation = RelatedObjectsHelper.getAnnotations(ontology, ioHelper, annotationString);  //f.getOWLAnnotation(f.getOWLAnnotationProperty(IRI.create(OWL.DEPRECATED)), f.getOWLLiteral(true));
      manager.addAxioms(ontology, axioms.stream().map(a -> a.getAnnotatedAxiom(annotation)).collect(Collectors.toSet()));
    } else {
      throw new IllegalArgumentException("Unknown operation type " + operation);
    }

    // Save the changed ontology and return the state
    CommandLineHelper.maybeSaveOutput(line, ontology);
    state.setOntology(ontology);
    return state;
  }
}