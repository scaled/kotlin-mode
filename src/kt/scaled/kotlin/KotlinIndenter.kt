//
// Scaled Kotlin Mode - a Scaled major mode for editing Kotlin code
// http://github.com/scaled/kotlin-mode/blob/master/LICENSE

package scaled.kotlin

import scaled.*
import scaled.code.Indenter
import scaled.util.Chars

class KotlinIndenter (buffer :Buffer, config: Config) : Indenter.ByBlock(buffer, config) {

  // TODO: make all the things configurable

  // override fun computeIndent (state :Indenter.State, base :Int, line :LineV, first :Int) :Int {
  //   // pop case statements out one indentation level
  //   if (line.matches(caseColonM, first)) return base - indentWidth()
  //   // bump extends/implements in two indentation levels
  //   else if (line.matches(extendsImplsM, first)) return base + 2*indentWidth()
  //   // otherwise do the standard business
  //   else return super.computeIndent(state, base, line, first)
  // }

  override fun computeCloseIndent (state :Indenter.BlockS, line :LineV, first :Int) :Int {
    // if the top of the stack is a switch + block, then skip both of those
    if (state.next() is SwitchS) return state.next().next().indent(config(), false)
    else return super.computeCloseIndent(state, line, first)
  }

  override fun createStater () = object : BlockStater() {
    override fun adjustStart (line :LineV, first :Int, last :Int, start :State) :State {
      // if this line opens a block or doc comment, push a state for it
      if (Indenter.countComments(line, first) > 0) {
        // if this is a doc comment which is followed by non-whitespace, then indent to match the
        // second star rather than the first
        return CommentS(if (line.matches(firstLineDocM, first)) 2 else 1, start)
      }
      // else if (config().apply(JavaConfig.INSTANCE.indentSwitchBlock) &&
      //          line.matches(switchM, first)) {
      //   return SwitchS(start)
      // }
      // otherwise leave the start as is
      return start
    }

    override fun adjustEnd (line :LineV, first :Int, last :Int, start :State, cur :State) :State {
      // if this line closes a doc/block comment, pop our comment state from the stack
      val ncur = if (Indenter.countComments(line, first) < 0) cur.popIf { it is CommentS } else cur

      // // determine whether this line is continued onto the next line (heuristically)
      // if (last >= 0) {
      //   val lastC = line.charAt(last)
      //   var isContinued :Boolean
      //   switch (lastC) {
      //     case '.': case '+': case '-': case '?': case '=': isContinued = true break
      //     case ':': isContinued = !line.matches(caseColonM, first) break
      //     default:  isContinued = false break
      //   }
      //   boolean inContinued = (cur instanceof ContinuedS)
      //   if (isContinued && !inContinued) return new ContinuedS(cur)
      //   else if (inContinued && !isContinued) return cur.next() // pop the ContinuedS
      // }

      // otherwise we are full of normalcy
      return ncur
    }

    override fun closeBlock (line :LineV, close :Char, col :Int, state :State) :State {
      val popped = super.closeBlock(line, close, col, state)
      // if there's a SwitchS on top of the stack after we pop a } block, pop it off too
      return if (close == '}' && popped is SwitchS) popped.next() else  popped
    }
  }

  protected class CommentS (val inset :Int, next :State) : Indenter.State(next) {
    override fun indent (config :Config, top :Boolean) = inset + next().indent(config, false)
    override fun show () = "CommentS($inset)"
  }

  // protected static class ContinuedS extends Indenter.State {
  //   public ContinuedS (State next) { super(next) }
  //   @Override public int indent (Config config, boolean top) {
  //     // if we're a continued statement directly inside an expr block, let the expr block dictate
  //     // alignment rather than our standard extra indents
  //     State n = next()
  //     return (n instanceof ExprS) ? n.indent(config, top) : super.indent(config, top)
  //   }
  //   @Override public String show () { return "ContinuedS" }
  // }

  protected class SwitchS (next :State) : Indenter.State(next) {
    override fun show () = "SwitchS"
  }

  // private final Matcher caseColonM = Matcher.regexp("(case\\s|default).*:")
  // private final Matcher extendsImplsM = Matcher.regexp("(extends|implements)\\b")
  // private final Matcher switchM = Matcher.regexp("switch\\b")

  private val firstLineDocM = Matcher.regexp("/\\*\\*\\s*\\S+")
}
