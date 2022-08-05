package org.obolibrary.robot;

import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.obolibrary.robot.checks.Violation;
import org.semanticweb.owlapi.model.OWLOntology;

@RunWith(Parameterized.class)
public class ConstraintsFixTest extends AbstractConstraintsTest {

  private static final String[] constraintNames = {
    "annotation_whitespace", "label_whitespace", "label_formatting"
  };

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return data(constraintNames);
  }

  public ConstraintsFixTest(
      final String constraintName,
      final OWLOntology input,
      final List<String> failingEntities,
      final OWLOntology inputRepaired) {
    super(constraintName, input, failingEntities, inputRepaired);
  }

  /** Test constraint violation fix */
  @Test
  public void testConstraintViolationFix() throws Exception {
    final Model model = QueryOperation.loadOntologyAsModel(input);
    final String query = loadResourceAsString("/repair_queries/" + constraintName + ".rq");
    QueryOperation.execUpdate(model, query);
    final OWLOntology o = QueryOperation.convertModel(model);
    Assert.assertTrue(DiffOperation.compare(inputRepaired, o, new PrintWriter(System.out)));
  }

  /** Test report queries */
  @Test
  public void testConstraintSatisfied() throws Exception {
    final Dataset dataset = QueryOperation.loadOntologyAsDataset(inputRepaired, false);

    final String query = loadResourceAsString("/report_queries/" + constraintName + ".rq");
    final List<Violation> violations =
        ReportOperation.getViolations(
            new IOHelper(), dataset, constraintName, query, Collections.emptyMap());

    final Set<String> actual =
        violations.stream().map(v -> v.entity.getIRI().toString()).collect(Collectors.toSet());
    Assert.assertEquals(Collections.emptySet(), actual);
  }
}
