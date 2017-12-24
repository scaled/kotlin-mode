//
// Scaled Kotlin Mode - a Scaled major mode for editing Kotlin code
// http://github.com/scaled/kotlin-mode/blob/master/LICENSE

package scaled.kotlin

import scaled.*
import scaled.code.CodeConfig.*
import scaled.grammar.GrammarPlugin

@Plugin(tag="textmate-grammar")
class KotlinGrammarPlugin : GrammarPlugin() {

  override fun grammars () = scaled.Map.builder<String,String>().
    put("source.kotlin", "Kotlin.ndf").
    build()

  override fun effacers () = Std.list(
    // Standard code colorizations
    effacer("comment.line", commentStyle()),
    effacer("comment.block", docStyle()),
    effacer("constant", constantStyle()),
    effacer("invalid", invalidStyle()),
    effacer("keyword", keywordStyle()),
    effacer("string", stringStyle()),

    // Kotlin code colorizations
    effacer("entity.name.package", moduleStyle()),
    effacer("entity.name.type.class", typeStyle()),
    effacer("storage.modifier", keywordStyle()),
    effacer("entity.name.function", functionStyle()),
    effacer("entity.name.variable", variableStyle()),
    effacer("storage.type", typeStyle()),
    effacer("entity.other.inherited-class", typeStyle()),
    effacer("keyword.control.flow", functionStyle()), // highlight these

    // TODO
    effacer("storage.type.annotation", preprocessorStyle()),

    // TODO: Doc colorizations
    effacer("markup.underline", preprocessorStyle())
  )

  override fun syntaxers () = Std.list(
    syntaxer("comment.line", Syntax.LineComment()),
    syntaxer("comment.block", Syntax.DocComment()),
    syntaxer("constant", Syntax.OtherLiteral()),
    syntaxer("string", Syntax.StringLiteral())
  )
}
