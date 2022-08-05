package org.obolibrary.robot;

import java.util.*;
import java.util.stream.Collectors;
import org.apache.jena.query.Dataset;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.obolibrary.robot.checks.Violation;
import org.semanticweb.owlapi.model.OWLOntology;

@RunWith(Parameterized.class)
public class ConstraintsTest extends AbstractConstraintsTest {

  private static final String[] constraintNames = {
    "annotation_whitespace", "label_whitespace", "label_formatting", "missing_label"
  };

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return data(constraintNames);
  }

  public ConstraintsTest(
      final String constraintName,
      final OWLOntology input,
      final List<String> failingEntities,
      final OWLOntology inputRepaired) {
    super(constraintName, input, failingEntities, inputRepaired);
  }

  /** Test report queries */
  @Test
  public void testConstraintViolations() throws Exception {
    final Dataset dataset = QueryOperation.loadOntologyAsDataset(input, false);

    final String query = loadResourceAsString("/report_queries/" + constraintName + ".rq");
    final List<Violation> violations =
        ReportOperation.getViolations(
            new IOHelper(), dataset, constraintName, query, Collections.emptyMap());

    final Set<String> expected = new HashSet<>(failingEntities);
    assert violations != null;
    final Set<String> actual =
        violations.stream().map(v -> v.entity.getIRI().toString()).collect(Collectors.toSet());
    Assert.assertEquals(expected, actual);
  }
}
