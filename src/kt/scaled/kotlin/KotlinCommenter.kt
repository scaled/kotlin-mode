//
// Scaled Kotlin Mode - a Scaled major mode for editing Kotlin code
// http://github.com/scaled/kotlin-mode/blob/master/LICENSE

package scaled.kotlin

import scaled.*
import scaled.code.CodeConfig.*
import scaled.code.Commenter
import scaled.util.Paragrapher

class KotlinCommenter : Commenter() {

  val openDocM = Matcher.exact("/**")
  val closeDocM = Matcher.exact("*/")
  val atCmdM = Matcher.regexp("@[a-z]+")

  fun inDoc (buffer :BufferV, p :Long) :Boolean {
    val line = buffer.line(p)
    return (
      // we need to be on doc-styled text...
      buffer.stylesNear(p).contains(docStyle()) &&
      // and not on the open doc (/**)
      !line.matches(openDocM, Loc.c(p)) &&
      // and not on or after the close doc (*/)
      (line.lastIndexOf(closeDocM, Loc.c(p)) == -1)
    )
  }

  fun insertDocPre (buffer :Buffer, p :Long) :Long {
    return buffer.insert(p, Line.apply(docPrefix()))
  }

  override fun linePrefix () = "//"
  override fun blockOpen () = "/*"
  override fun blockClose () = "*/"
  override fun blockPrefix () = "*"
  override fun docPrefix () = "*"

  override fun mkParagrapher (syn :Syntax, buf :Buffer) :Paragrapher {
    return object : Paragrapher(syn, buf) {
      fun isAtCmdLine (line :LineV) = line.matches(atCmdM, commentStart(line))
      // don't extend paragraph upwards if the current top is an @cmd
      override fun canPrepend (row :Int) = super.canPrepend(row) && !isAtCmdLine(line(row+1))
      // don't extend paragraph downwards if the new line is at an @cmd
      override fun canAppend (row :Int) = super.canAppend(row) && !isAtCmdLine(line(row))
      // have to duplicate this due to kotlinc bug re: inheriting from inner class
      override fun isDelim (row :Int) :Boolean {
        val l = line(row)
        return commentStart(l) == l.length
      }
    }
  }

  override fun commentDelimLen (line :LineV, col :Int) :Int {
    if (line.matches(openDocM, col)) return openDocM.matchLength()
    else if (line.matches(closeDocM, col)) return closeDocM.matchLength()
    else return super.commentDelimLen(line, col)
  }
}
