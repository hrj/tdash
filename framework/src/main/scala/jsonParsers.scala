package bhoot

import scala.util.parsing.combinator._

object JSON extends JavaTokenParsers { 
  def obj: Parser[Map[String, Any]] = 
    "{"~> repsep(member, ",") <~"}" ^^ (Map() ++ _) 

  def arr: Parser[List[Any]] = 
    "["~> repsep(value, ",") <~"]" 

  def member: Parser[(String, Any)] = 
    stringLiteral~":"~value ^^ 
    { case name~":"~value => (name, value) } 

  def value: Parser[Any] = ( 
      obj 
      | arr 
      | stringLiteral
      | floatingPointNumber
      | wholeNumber
      | "null" ^^ (x => null) 
      | "true" ^^ (x => true) 
      | "false" ^^ (x => false) 
      ) 

  override def stringLiteral =
    ("\"" + """[^"\\]*(\\.[^"\\]*)*""" + "\"").r ^^ {case s => if(s.startsWith("\"")) s.slice(1,s.length-1) else s}

  def parse(reader:java.io.Reader) = {
    parseAll(value, reader)
  }
}
