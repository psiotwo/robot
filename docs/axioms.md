# Axioms

## Contents

1. [Overview](#overview)
2. [Examples](#examples)

## Overview

The `axioms` command allows you to perform various operations on the selected axioms from an ontology, as specified by
the `--operation` option. Currently, two operations are supported:
- `remove` - removes them from the ontology
- `annotate` - annotates them as specified by the `annotation` argument, e.g. '"owl:deprecated='true'^^xsd:boolean"'

The `--select` option lets you specify an annotation selector to pick axioms the operation is applied for. If not specified, all axioms are taken:
- `CURIE=CURIE`, e.g. `rdfs:seeAlso=obo:EX_123`
- `CURIE=<IRI>`, e.g. `rdfs:seeAlso=<http://purl.obolibrary.org/obo/EX_123>`
- `CURIE='literal'`, e.g. `rdfs:label='example label'`
- `CURIE='literal'^^datatype`, e.g. `rdfs:label='example label'^^xsd:string`
- `CURIE='literal'@language`, e.g. `rdfs:label='example label'@en`
- `CURIE=^^datatype`, e.g. `rdfs:label=^^xsd:string`
- `CURIE=@language`, e.g. `rdfs:label=@en`
- `CURIE=~'regex pattern'`, e.g. `rdfs:label=~'example.*'`

## Examples

Remove all axioms which are deprecated, see [#988](https://github.com/ontodev/robot/issues/988):

    robot axioms --input ontology-github-988.owl \
      --select "owl:deprecated='true'^^xsd:boolean" \
      --operation remove \
      --output results/ontology-github-988.owl

Remove all DisjointWith axioms which are deprecated:

    robot axioms --input ontology-github-988-disjointclasses.owl \
      --select "owl:deprecated='true'^^xsd:boolean" \
      --operation remove \
      --output results/ontology-github-988-disjointclasses.owl

Mark all axioms in an ontology as deprecated:

    robot axioms --input ontology-github-988.owl \
      --operation annotate \
      --annotation "owl:deprecated='true'^^xsd:boolean" \
      --output results/ontology-github-988.owl