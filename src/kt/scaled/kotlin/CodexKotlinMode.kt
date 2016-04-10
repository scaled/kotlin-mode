//
// Scaled Kotlin Mode - a Scaled major mode for editing Kotlin code
// http://github.com/scaled/kotlin-mode/blob/master/LICENSE

package scaled.kotlin

import codex.model.Def
import codex.model.Kind
import scaled.*
import scaled.project.CodexMinorMode
import scaled.util.Errors

@Minor(name="codex-kotlin",
       tags=arrayOf("kotlin"),
       desc="A minor mode which enhances Kotlin mode with Codex information.")
class CodexKotlinMode (env :Env) : CodexMinorMode(env) {

  override fun keymap () = super.keymap().
    bind("codex-import-type", "C-c C-i")

  @Fn("Queries for a type (completed by the project's Codex) and adds an import for it.")
  fun codexImportType () { codexRead("Type:", Kind.TYPE) { insertImport(it) }}

  private val importM = Matcher.regexp("^import ")
  private val packageM = Matcher.regexp("^package ")
  private val firstDefM = Matcher.regexp("\\b(class|interface|object)\\b")

  // TODO:
  // - handle groups of imports separated by spaces
  private fun insertImport (df :Def) {
    val fqName = df.fqName()
    val text = "import $fqName"

    // first figure out where we're going to stop looking
    val buffer = buffer()
    var firstDef = buffer.findForward(firstDefM, buffer.start(), buffer.end())
    if (firstDef == Loc.None()) firstDef = buffer.end()

    // look for an existing "import " statement in the buffer and scan down from there to find the
    // position at which to insert the new statement
    fun loop (prev :Long) :Long {
      val next = buffer.findForward(importM, Loc.`nextStart$extension`(prev), firstDef)
      // if we see no more import statements...
      return if (next == Loc.None()) {
        // if we saw at least one import statement, then insert after the last one we saw
        if (prev != buffer.start()) Loc.`nextStart$extension`(prev)
        // otherwise fail the search and fall back to inserting after 'package'
        else Loc.None()
      }
      else {
        val ltext = buffer.line(next).asString()
        // if we have this exact import, abort (we'll catch and report this below)
        if (ltext == text) throw Errors.feedback("$fqName already imported.")
        // if our import sorts earlier than this import, insert here
        else if (text < ltext) next
        // otherwise check the next import statement
        else loop(next)
      }
    }

    var loc = loop(buffer.start())
    // if we failed to find existing import statements, look for a package statement
    if (loc == Loc.None()) {
      loc = buffer.findForward(packageM, buffer.start(), firstDef)
      if (loc == Loc.None()) {
        // fuck's sake, put the import at the top of the file (with a blank line after)
        buffer.insert(buffer.start(), Line.fromText("$text\n\n"))
      } else {
        // insert a blank line after 'package' and then our import
        buffer.insert(Loc.`nextStart$extension`(loc), Line.fromText("\n$text\n"))
      }
    } else {
      // put the import at the specified location
      buffer.insert(loc, Line.fromText("$text\n"))
    }
  }
}
