
import groovy.xml.XmlSlurper

input = args[0]
text = new File(input).text

println "# Reading $input ..."

sbml = new XmlSlurper().parseText(text)
  .declareNamespace(celldesigner: 'http://www.sbml.org/2001/ns/celldesigner')
  .declareNamespace(rdf: 'http://www.w3.org/1999/02/22-rdf-syntax-ns#')
  .declareNamespace(bqbiol: 'http://biomodels.net/biology-qualifiers/')

println "@prefix dc:      <http://purl.org/dc/elements/1.1/> ."
println "@prefix dcterms: <http://purl.org/dc/terms/> ."
println "@prefix rdf:     <http://www.w3.org/1999/02/22-rdf-syntax-ns#> ."
println "@prefix rdfs:    <http://www.w3.org/2000/01/rdf-schema#> ."
println "@prefix skos:    <http://www.w3.org/2004/02/skos/core#> ."
println "@prefix wp:      <http://vocabularies.wikipathways.org/wp#> ."
println ""

pwURL = "https://covid19map.elixir-luxembourg.org/minerva/submap/" + sbml.model.@id

println "<$pwURL>\n        rdf:type skos:Collection , wp:Pathway ;"
println "        dc:source              \"Minerva\" ."
println ""

for (species : sbml.model.listOfSpecies.species) {
  speciesURL = pwURL + "/species/" + species.@id;
  specificType = ""
  if (species.annotation.'celldesigner:extension'.'celldesigner:speciesIdentity'.'celldesigner:class') {
    type = species.annotation.'celldesigner:extension'.'celldesigner:speciesIdentity'.'celldesigner:class'
    if (type == "COMPLEX") specificType += ", wp:Complex"
    if (type == "PROTEIN") specificType += ", wp:Protein"
  }
  println "<$speciesURL>\n        rdf:type wp:DataNode${specificType} ;"
  ncbiDone = false
  ensemblDone = false
  hgncDone = false
  mainExtDone = false
  for (annotation : species.annotation.'rdf:RDF'.'rdf:Description'.'bqbiol:isDescribedBy') {
    extID = annotation.'rdf:Bag'.'rdf:li'.'@rdf:resource'
    if (("" + extID).startsWith("urn:miriam:ensembl:") && !ensemblDone) {
      ensemblDone = true
      ensID = ("" + extID).substring(19)
      if (!mainExtDone) {
        mainExtDone = true
        println "        dc:source           \"Ensembl\" ;"
        println "        dcterms:identifier  \"${ensID}\" ;"
      }
      println "        wp:bdbEnsembl       <https://identifiers.org/ensembl/${ensID}> ;"
    } else if (("" + extID).startsWith("urn:miriam:ncbigene:") && !ncbiDone) {
      ncbiDone = true
      ncbiID = ("" + extID).substring(20)
      if (!mainExtDone) {
        mainExtDone = true
        println "        dc:source           \"Entrez Gene\" ;"
        println "        dcterms:identifier  \"${ncbiID}\" ;"
      }
      println "        wp:bdbEntrezGene    <https://identifiers.org/ncbigene/${ncbiID}> ;"
    } else if (("" + extID).startsWith("urn:miriam:hgnc.symbol:") && !hgncDone) {
      hgncDone = true
      hgncID = ("" + extID).substring(23)
      println "        rdfs:label          \"${hgncID}\" ;"
    }
  }
  println "        dcterms:isPartOf    <$pwURL> ."
  println ""
}

for (reaction : sbml.model.listOfReactions.reaction) {
  interactionURL = pwURL + "/interaction/" + reaction.@id
  println "<$interactionURL>\n        rdf:type wp:DirectedInteraction , wp:Interaction ;"
  sources = ""
  targets = ""
  participants = ""
  for (reactant : reaction.listOfReactants.speciesReference) {
    if (participants.length() > 0) participants += ", "
    if (sources.length() > 0) sources += ", "
    speciesURL = pwURL + "/species/" + reactant.@species
    participants += "<$speciesURL>"
    sources += "<$speciesURL>"
  }
  for (product : reaction.listOfProducts.speciesReference) {
    if (participants.length() > 0) participants += ", "
    if (targets.length() > 0) targets += ", "
    speciesURL = pwURL + "/species/" + product.@species
    participants += "<$speciesURL>"
    targets += "<$speciesURL>"
  }
  if (participants.length() > 0) println "        wp:participants     $participants ;"
  if (sources.length() > 0)    println "        wp:source           $sources ;"
  if (targets.length() > 0)    println "        wp:target           $targets ;"
  println "        dcterms:isPartOf    <$pwURL> ."
  println ""
}
