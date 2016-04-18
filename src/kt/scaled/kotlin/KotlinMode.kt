//
// Scaled Kotlin Mode - a Scaled major mode for editing Kotlin code
// http://github.com/scaled/kotlin-mode/blob/master/LICENSE

package scaled.kotlin

import scaled.*
import scaled.List
import scaled.code.CodeConfig
import scaled.code.CodeConfig.*
import scaled.code.Commenter
import scaled.code.Indenter
import scaled.grammar.Grammar
import scaled.grammar.GrammarCodeMode
import scaled.grammar.GrammarConfig
import scaled.grammar.Selector
import scaled.util.Paragrapher

@Major(name="kotlin",
       tags=arrayOf("code", "project", "kotlin"),
       pats=arrayOf(".*\\.kt"),
       ints=arrayOf("kotlin"),
       desc="A major mode for editing Kotlin language source code.")
class KotlinMode (env :Env) : GrammarCodeMode(env) {

  companion object : Config.Defs() {
    @Var("If true, switch blocks are indented one step.")
    val indentSwitchBlock = key(false)

    // map TextMate grammar scopes to Scaled style definitions
    val effacers = Std.list(
      // Standard code colorizations
      GrammarConfig.effacer("comment.line", commentStyle()),
      GrammarConfig.effacer("comment.block", docStyle()),
      GrammarConfig.effacer("constant", constantStyle()),
      GrammarConfig.effacer("invalid", invalidStyle()),
      GrammarConfig.effacer("keyword", keywordStyle()),
      GrammarConfig.effacer("string", stringStyle()),

      // Kotlin code colorizations
      GrammarConfig.effacer("entity.name.package", moduleStyle()),
      GrammarConfig.effacer("entity.name.type.class", typeStyle()),
      GrammarConfig.effacer("storage.modifier", keywordStyle()),
      GrammarConfig.effacer("entity.name.function", functionStyle()),
      GrammarConfig.effacer("entity.name.variable", variableStyle()),
      GrammarConfig.effacer("storage.type", typeStyle()),
      GrammarConfig.effacer("entity.other.inherited-class", typeStyle()),
      GrammarConfig.effacer("keyword.control.flow", functionStyle()), // highlight these

      // TODO
      GrammarConfig.effacer("storage.type.annotation", preprocessorStyle()),

      // TODO: Doc colorizations
      GrammarConfig.effacer("markup.underline", preprocessorStyle())
    )

    // map TextMate grammar scopes to Scaled syntax definitions
    val syntaxers = Std.list(
      GrammarConfig.syntaxer("comment.line", Syntax.LineComment()),
      GrammarConfig.syntaxer("comment.block", Syntax.DocComment()),
      GrammarConfig.syntaxer("constant", Syntax.OtherLiteral()),
      GrammarConfig.syntaxer("string", Syntax.StringLiteral())
    )

    val grammars = resource(Std.seq("Kotlin.ndf"), Grammar.parseNDFs())

    // override fun key (p0: kotlin.Boolean) = super.key(p0 as java.lang.Boolean)
  }

  override fun configDefs () :List<Config.Defs>? = super.configDefs().cons(Companion)

  override fun grammars () = Companion.grammars.get()
  override fun effacers () = Companion.effacers
  override fun syntaxers () = Companion.syntaxers

  override fun createIndenter () = KotlinIndenter(config())

  // TODO: val
  override fun commenter () = object : Commenter() {
    override fun linePrefix () = "//"
    override fun blockOpen () = "/*"
    override fun blockClose () = "*/"
    override fun blockPrefix () = "*"
    override fun docOpen () = "/**"
  }
}
