
@Grab(group='jakarta.activation', module='jakarta.activation-api', version='1.2.2')
@Grab(group='io.github.egonw.bacting', module='managers-bridgedb', version='0.0.34')
@Grab(group='io.github.egonw.bacting', module='managers-ui', version='0.0.34')

workspaceRoot = ".."
bioclipse = new net.bioclipse.managers.BioclipseManager(workspaceRoot);
bridgedb = new net.bioclipse.managers.BridgedbManager(workspaceRoot);
mapper = new org.bridgedb.IDMapperStack()
mapper.addIDMapper(bridgedb.loadRelationalDatabase(bioclipse.fullPath("/minerva2rdf/Hs_Derby_Ensembl_104.bridge")))
mapper.addIDMapper(bridgedb.loadRelationalDatabase(bioclipse.fullPath("/minerva2rdf/complexes_20200510.bridge")))
mapper.addIDMapper(bridgedb.loadRelationalDatabase(bioclipse.fullPath("/minerva2rdf/humancorona-2021-11-27.bridge")))

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
    if (type == "GENE") specificType += ", wp:GeneProduct"
    if (type == "RNA") specificType += ", wp:Rna"
    if (type == "PROTEIN") specificType += ", wp:Protein"
    if (type == "SIMPLE_MOLECULE") specificType += ", wp:Metabolite"
    if (type == "DRUG") specificType += ", wp:Metabolite"
    if (type == "ION") specificType += ", wp:Metabolite"
  }
  println "<$speciesURL>\n        rdf:type wp:DataNode${specificType} ;"
  name = "" + species.@name
  if (name != "") {
    name = name.replaceAll("_slash_","/")
    println "        rdfs:label \"${name}\" ;"
  } else if (species.annotation.'celldesigner:extension'.'celldesigner:speciesIdentity'.'celldesigner:name') {
    name = species.annotation.'celldesigner:extension'.'celldesigner:speciesIdentity'.'celldesigner:name'
    name = name.replace("_slash_","/")
    if (name != "") println "        rdfs:label \"${name}\" ;"
  }
  ncbiDone = false
  ensemblDone = false
  hgncDone = false
  mainExtDone = false
  extIDref = null
  for (annotation : species.annotation.'rdf:RDF'.'rdf:Description'.'bqbiol:isDescribedBy') {
    extID = annotation.'rdf:Bag'.'rdf:li'.'@rdf:resource'
    if (("" + extID).startsWith("urn:miriam:ensembl:") && !ensemblDone) { // only one Ensembl ID
      ensemblDone = true
      ensID = ("" + extID).substring(19)
      if (!mainExtDone) {
        mainExtDone = true
        extIDref = bridgedb.xref(ensID, "En")
        println "        dc:source           \"Ensembl\" ;"
        println "        dcterms:identifier  \"${ensID}\" ;"
      }
      println "        wp:bdbEnsembl       <https://identifiers.org/ensembl/${ensID}> ;"
    } else if (("" + extID).startsWith("urn:miriam:ncbigene:") && !ncbiDone) { // only one NCBI Gene ID
      ncbiDone = true
      ncbiID = ("" + extID).substring(20)
      if (!mainExtDone) {
        mainExtDone = true
        extIDref = bridgedb.xref(ncbiID, "L")
        println "        dc:source           \"Entrez Gene\" ;"
        println "        dcterms:identifier  \"${ncbiID}\" ;"
      }
      println "        wp:bdbEntrezGene    <https://identifiers.org/ncbigene/${ncbiID}> ;"
    } else if (("" + extID).startsWith("urn:miriam:hgnc.symbol:") && !hgncDone) { // only one HGNC ID
      hgncDone = true
      hgncID = ("" + extID).substring(23)
      println "        wp:bdbHgncSymbol    <https://identifiers.org/hgnc.symbol/${hgncID}> ;"
      println "        rdfs:label          \"${hgncID}\" ;"
    }
  }
  if (extIDref != null) {
    println "        # mapped identifiers, starting from ${extIDref}"
    if (!ensemblDone) {
      mappings = bridgedb.map(mapper, extIDref, "En")
      if (mappings != null && mappings.size() > 0) {
        mapping = mappings.iterator().next()
        println "        wp:bdbEnsembl    <https://identifiers.org/ensembl/${mapping.id}> ;"
      }
    }
    if (!hgncDone) {
      mappings = bridgedb.map(mapper, extIDref, "H")
      if (mappings != null && mappings.size() > 0) {
        mapping = mappings.iterator().next()
        println "        wp:bdbHgncSymbol    <https://identifiers.org/hgnc.symbol/${mapping.id}> ;"
      }
    }
    {
      mappings = bridgedb.map(mapper, extIDref, "S")
      for (mapping : mappings) {
        println "        wp:bdbUniprot    <https://identifiers.org/uniprot/${mapping.id}> ;"
      }
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
