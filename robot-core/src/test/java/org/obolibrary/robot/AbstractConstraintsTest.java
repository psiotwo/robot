package org.obolibrary.robot;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.semanticweb.owlapi.model.OWLOntology;

@RunWith(Parameterized.class)
public class AbstractConstraintsTest extends CoreTest {

  public static Collection<Object[]> data(String[] constraintNames) {
    return Arrays.stream(constraintNames)
        .map(
            constraintName -> {
              try {
                final String inputRepaired =
                    "/constraints/" + constraintName + "/input-repaired.owl";
                return new Object[] {
                  constraintName,
                  loadOntology("/constraints/" + constraintName + "/input.owl"),
                  loadListFromFile("/constraints/" + constraintName + "/input-violations.csv"),
                  AbstractConstraintsTest.class.getResource(inputRepaired) == null
                      ? null
                      : loadOntology(inputRepaired)
                };
              } catch (IOException e) {
                e.printStackTrace();
                return null;
              }
            })
        .collect(Collectors.toList());
  }

  protected final String constraintName;
  protected final OWLOntology input;
  protected final List<String> failingEntities;
  protected final OWLOntology inputRepaired;

  public AbstractConstraintsTest(
      final String constraintName,
      final OWLOntology input,
      final List<String> failingEntities,
      final OWLOntology inputRepaired) {
    this.constraintName = constraintName;
    this.input = input;
    this.failingEntities = failingEntities;
    this.inputRepaired = inputRepaired;
  }

  public static List<String> loadListFromFile(final String resource) {
    final Scanner scanner =
        new Scanner(AbstractConstraintsTest.class.getResourceAsStream(resource)).useDelimiter("\n");

    final List<String> entities = new ArrayList<>();
    while (scanner.hasNext()) {
      entities.add(scanner.next());
    }
    return entities;
  }

  public String loadResourceAsString(final String path) {
    final InputStream is = getClass().getResourceAsStream(path);
    return new Scanner(is, "UTF-8").useDelimiter("" + Character.LINE_SEPARATOR).next();
  }
}
