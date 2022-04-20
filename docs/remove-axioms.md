# Remove Axioms

## Contents

1. [Overview](#overview)
2. [Examples](#examples)

## Overview

The `remove-axioms` command allows you to remove selected axioms from an ontology:
The `--select` option lets you specify an annotation selector:

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

    robot remove --input ontology-github-988.owl \
      --select "owl:deprecated='true'^^xsd:boolean" \
      --output results/ontology-github-988.owl
