//
// Scaled Kotlin Mode - a Scaled major mode for editing Kotlin code
// http://github.com/scaled/kotlin-mode/blob/master/LICENSE

package scaled.kotlin

import scaled.*
import scaled.List
import scaled.code.Commenter
import scaled.code.Indenter
import scaled.grammar.GrammarCodeMode

@Major(name="kotlin",
       tags=arrayOf("code", "project", "kotlin"),
       pats=arrayOf(".*\\.kt", ".*\\.kts"),
       ints=arrayOf("kotlin", "kotlins"),
       desc="A major mode for editing Kotlin language source code.")
class KotlinMode (env :Env) : GrammarCodeMode(env) {

  companion object : Config.Defs() {
    @Var("If true, switch blocks are indented one step.")
    val indentSwitchBlock = key(false)

    // override fun key (p0: kotlin.Boolean) = super.key(p0 as java.lang.Boolean)
  }

  override fun configDefs () :List<Config.Defs>? = super.configDefs().cons(Companion)
  override fun langScope () = "source.kotlin"
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
