
import groovy.xml.XmlSlurper

input = args[0]
text = new File(input).text

println "# Reading $input ..."

sbml = new XmlSlurper().parseText(text)

println "@prefix dc:      <http://purl.org/dc/elements/1.1/> ."
println "@prefix dcterms: <http://purl.org/dc/terms/> ."
println "@prefix skos:    <http://www.w3.org/2004/02/skos/core#> ."
println "@prefix wp:      <http://vocabularies.wikipathways.org/wp#> ."
println ""

pwURL = "https://covid19map.elixir-luxembourg.org/minerva/submap/" + sbml.model.@id

println "<$pwURL>\n        rdf:type skos:Collection , wp:Pathway ;"
println "        dc:source              \"Minerva\" ."
println ""

for (reaction : sbml.model.listOfReactions.reaction) {
  interactionURL = pwURL + "/interaction/" + reaction.@id
  println "<$interactionURL>\n        rdf:type wp:DirectedInteraction , wp:Interaction ;"
  println "        dcterms:isPartOf <$pwURL> ."
  println ""
}
