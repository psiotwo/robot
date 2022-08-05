package org.obolibrary.robot;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.reasoner.rulesys.builtins.Print;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.obolibrary.robot.checks.Violation;
import org.semanticweb.owlapi.model.OWLOntology;

@RunWith(Parameterized.class)
public class ConstraintsTest extends CoreTest {

  private static final String[] constraintNames = {
    "annotation_whitespace",
    "missing_label"
  };

  @Parameterized.Parameters(name = "testReport - {0}")
  public static Collection<Object[]> data() {
    return Arrays.stream(constraintNames).map(constraintName -> {
      try {
        final String inputRepaired = "/constraints/" + constraintName + "/input-repaired.owl";
        return new Object[]{
          constraintName,
          loadOntology("/constraints/" + constraintName + "/input.owl"),
          loadListFromFile("/constraints/" + constraintName + "/input-violations.csv"),
          ConstraintsTest.class.getResource(inputRepaired) == null ? null :
            loadOntology(inputRepaired)
        };
      } catch (IOException e) {
        e.printStackTrace();
        return null;
      }
    }).collect(Collectors.toList());
  }

  private final String constraintName;
  private final OWLOntology input;
  private final List<String> failingEntities;
  private final OWLOntology inputRepaired;

  public ConstraintsTest(
    final String constraintName, final OWLOntology input, final List<String> failingEntities, final OWLOntology inputRepaired) {
    this.constraintName = constraintName;
    this.input = input;
    this.failingEntities = failingEntities;
    this.inputRepaired = inputRepaired;
  }

  private static List<String> loadListFromFile(final String resource) {
    final Scanner scanner = new Scanner(ConstraintsTest.class.getResourceAsStream(resource))
      .useDelimiter("\n");

    final List<String> entities = new ArrayList<>();
    while (scanner.hasNext()) {
      entities.add(scanner.next());
    }
    return entities;
  }

  /**
   * Test report queries
   */
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

  private String loadResourceAsString(final String path) {
    final InputStream is = getClass().getResourceAsStream(path);
    return new Scanner(is, "UTF-8").useDelimiter("" + Character.LINE_SEPARATOR).next();
  }

  /**
   * Test constraint violation fix
   */
  @Test
  public void testConstraintViolationFix() throws Exception {
    if (inputRepaired == null) {
      return;
    }

    final Model model = QueryOperation.loadOntologyAsModel(input);

    final String query = loadResourceAsString("/repair_queries/" + constraintName + ".rq");

//    final Model writableModel = ModelFactory.createDefaultModel().add(model);
    QueryOperation.execUpdate(model, query);

    final OWLOntology o = QueryOperation.convertModel(model);
    Assert.assertTrue(DiffOperation.compare(inputRepaired, o, new PrintWriter(System.out)));
  }
}
