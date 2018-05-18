package org.obolibrary.robot;

import com.google.common.collect.Sets;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

/** Tests for FilterOperation. */
public class FilterOperationTest extends CoreTest {

  private final IOHelper ioHelper = new IOHelper();

  private final OWLDataFactory df = OWLManager.getOWLDataFactory();

  private final Set<Class<? extends OWLAxiom>> allAxioms = Sets.newHashSet(OWLAxiom.class);

  /**
   * Test filtering for all entities. Result should be the same as input.
   *
   * @throws IOException on issue reading or writing
   * @throws OWLOntologyCreationException on issue creating new ontology
   */
  @Test
  public void testFilterAll() throws IOException, OWLOntologyCreationException {
    OWLOntology ontology = loadOntology("/uberon.owl");
    OWLOntology filteredOntology =
        FilterOperation.filter(ontology, OntologyHelper.getEntities(ontology), allAxioms, true);
    assertIdentical("/uberon.owl", filteredOntology);
  }

  /**
   * Test filtering by an annotation.
   *
   * @throws IOException on issue reading or writing
   * @throws OWLOntologyCreationException on issue creating new ontology
   */
  @Test
  public void testFilterByAnnotation() throws IOException, OWLOntologyCreationException {
    OWLOntology ontology = loadOntology("/uberon.owl");
    Set<OWLAnnotation> annotations = new HashSet<>();
    annotations.add(
        df.getOWLAnnotation(
            df.getOWLAnnotationProperty(ioHelper.createIRI("rdfs:label")),
            df.getOWLLiteral("articular system")));
    Set<OWLEntity> entities = RelatedEntitiesHelper.getAnnotated(ontology, annotations);

    OWLOntology filteredOntology = FilterOperation.filter(ontology, entities, allAxioms, true);
    OntologyHelper.trimDangling(filteredOntology);
    assertIdentical("/filter_class.owl", filteredOntology);
  }

  /**
   * Tests filtering for one class.
   *
   * @throws IOException on issue reading or writing
   * @throws OWLOntologyCreationException on issue creating new ontology
   */
  @Test
  public void testFilterClass() throws IOException, OWLOntologyCreationException {
    OWLOntology ontology = loadOntology("/uberon.owl");
    Set<OWLEntity> entities = new HashSet<>();
    entities.add(df.getOWLClass(ioHelper.createIRI("UBERON:0004770")));

    OWLOntology filteredOntology = FilterOperation.filter(ontology, entities, allAxioms, true);
    OntologyHelper.trimDangling(filteredOntology);
    assertIdentical("/filter_class.owl", filteredOntology);
  }

  /**
   * Test filtering for descendants of a class.
   *
   * @throws IOException on issue reading or writing
   * @throws OWLOntologyCreationException on issue creating new ontology
   */
  @Test
  public void testFilterDescendants() throws IOException, OWLOntologyCreationException {
    OWLOntology ontology = loadOntology("/uberon.owl");
    Set<OWLEntity> entities =
        RelatedEntitiesHelper.getRelated(
            ontology,
            Sets.newHashSet(df.getOWLClass(ioHelper.createIRI("UBERON:0004905"))),
            RelationType.DESCENDANTS);

    OWLOntology filteredOntology = FilterOperation.filter(ontology, entities, allAxioms, true);
    OntologyHelper.trimDangling(filteredOntology);
    assertIdentical("/filter_descendants.owl", filteredOntology);
  }
}
