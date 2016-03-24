//
// Scaled Kotlin Mode - a Scaled major mode for editing Kotlin code
// http://github.com/scaled/kotlin-mode/blob/master/LICENSE

package scaled.kotlin

import scaled.*
import scaled.code.Indenter
import scaled.util.Chars

class KotlinIndenter (config: Config) : Indenter.ByBlock(config) {

  // TODO: make all the things configurable

  override fun createStater () = object : BlockStater() {
    private var opensSLB = false
    private var slbExprOpen = -1
    private var slbExprClose = -1
    private var slbExpectsPair = false

    override fun adjustStart (line :LineV, first :Int, last :Int, start :State) :State {
      // reset our SLB tracking state
      slbExprOpen = -1 ; slbExprClose = -1 ; slbExpectsPair = false
      // if we're looking at an SLB, push a state for it
      opensSLB = line.matches(singleLineBlockM, first)
      if (opensSLB) {
        val token = singleLineBlockM.group(1)
        val nstate = SingleBlockS(token, first, start)
        // if this SLB has no associated expression (else or a do); set the expression open/close
        // column to the end of the token so that the "pop on later block" code works properly
        if (nstate.lacksExpr()) {
          slbExprOpen = first+token.length ; slbExprClose = slbExprOpen
        }
        // if this is an 'if' or 'else if', or a 'do', we want to know whether or not to expect to
        // see a subsequent 'else' or 'while' so that we can determine if this statement should
        // terminate a continued statement chain; we check to see whether that expected pair
        // already occurs on this same line, in which case we don't expect it later; note that
        // it's possible for an 'if' or 'else if' to simply not be followed by an 'else', and in
        // that case we can potentially do the wrong thing, but there's only so much we can do
        // without a full fledged parser
        slbExpectsPair = nstate.expectsPair(line)
        return nstate
      }
      // if this line opens a block or doc comment, push a state for it
      else if (countComments(line, first) > 0) {
        // if this is a doc comment which is followed by non-whitespace, then indent to match the
        // second star rather than the first
        return CommentS(if (line.matches(firstLineDocM, first)) 2 else 1, start)
      }
      // otherwise leave the start as is
      else return start
    }

    override fun adjustEnd (line :LineV, first :Int, last :Int, start :State, cur :State) :State {
      // if this line closes a doc/block comment, pop our comment state from the stack
      var end = if (countComments(line, first) < 0) cur.popIf { it is CommentS } else cur

      // if the last non-ws-non-comment char is beyond our SLB condition expression then pop the SLB
      // state because the "body" was on the same line (this is normally done when we see any sort
      // of bracket after our SLB expr, but it's possible that the SLB body contains no brackets, so
      // we catch that case here)
      if (opensSLB && last > slbExprClose) {
        end = end.popIf { it is SingleBlockS }
        opensSLB = false
      }

      // if the top of the stack is a BlockS but the end of the line is -> then we're in a lambda
      // and need to adjust the BlockS to let it know that it actually should trigger indent
      if (end is BlockS) {
        val arrowStart = last+1-lambdaArrowM.show().length
        if (arrowStart >= 0 && line.matches(lambdaArrowM, arrowStart)) end = end.makeEOL()
      }

      // if this line is blank or contains only comments; do not mess with our "is continued or
      // not" state; wait until we get to a line with something actually on it
      if (line.synIndexOf(sfun { !it.isComment }, first) == -1) return end

      // determine (heuristically) whether this line appears to be a complete statement
      val isContinued = (last >= 0) && contChars.indexOf(line[last]) >= 0
      val isComplete = !(isContinued || slbExpectsPair || end is BlockS || end is ExprS)

      // if we appear to be a complete statement, pop any continued statement state off the stack
      return if (isComplete) {
        end = end.popIf { it is ContinuedS }
        // if we didn't just open an SLB and we're a complete statement, then pop any SLB because
        // this was the single line body of our single line block
        if (!opensSLB) end.popIf { it is SingleBlockS }
        else end
      }
      // if we're not already a continued statement, we may need to start being so
      else if (isContinued) ContinuedS(end.popIf { it is ContinuedS })
      // otherwise stick with what we have
      else end
    }

    override fun openBlock (line :LineV, open :Char, close :Char, col :Int, state :State) :State {
      var top = state
      if (opensSLB) {
        // if we're processing an SLB and this is the first block on the line, note its info
        if (slbExprOpen == -1) slbExprOpen = col
        // if we're opening another block after our SLB token's expression block, then pop the SLB
        // state because we're either opening a multi-line block or we're seeing an expression
        // which is cuddled onto the same line as the SLB; in either case we don't want our SLB
        // state to cause the next line to be indented
        else if (slbExprClose != -1) {
          top = top.popIf { it is SingleBlockS }
          opensSLB = false
        }
      }
      return super.openBlock(line, open, close, col, top)
    }

    override fun closeBlock (line :LineV, close :Char, col :Int, state :State) :State {
      // if we're closing the bracketed expr that goes along with our SLB, note the close column
      if (opensSLB && state is ExprS && state.col() == slbExprOpen) slbExprClose = col
      return super.closeBlock(line, close, col, state)
      // // if there's a SwitchS on top of the stack after we pop a } block, pop it off too
      // return if (close == '}' && popped is SwitchS) popped.next() else  popped
    }

    protected val contChars = ".+-=:"
  }

  protected class CommentS (val inset :Int, next :State) : Indenter.State(next) {
    override fun indent (config :Config, top :Boolean) = inset + next().indent(config, false)
    override fun show () = "CommentS($inset)"
  }

  protected class ContinuedS (next :State) : State(next) {
    override fun show () = "ContinuedS"
  }

  protected class SingleBlockS (val token :String, val col :Int, next :State) : State(next) {
    fun expectsPair (line :LineV) = when(token) {
      // if our if or else if is followed by an else on the same line, we're already paired
      "if", "else if" -> {
        val ii = line.lastIndexOf(elseM)
        // no else, we expect one OR  the else we saw was actually an else if
        if (ii == -1) true else (ii == line.lastIndexOf(elseIfM))
      }
      "do" -> line.indexOf(whileM) == -1
      else -> false
    }
    fun lacksExpr () = token == "else" || token == "do"
    // if the single-block state is on the top of the stack, then we're in the line immediately
    // following the single-block statement, so we want to indent
    override fun indent (config :Config, top :Boolean) =
      (if (top) indentWidth(config) else 0) + next().indent(config, top)
    override fun show () = "SingleBlockS($token, $col)"

    companion object {
      private val elseIfM = Matcher.regexp("""\belse\s+if\b""")
      private val elseM = Matcher.regexp("""\belse\b""")
      private val whileM = Matcher.regexp("""\bwhile\b""")
    }
  }

  private val firstLineDocM = Matcher.regexp("/\\*\\*\\s*\\S+")
  private val lambdaArrowM  = Matcher.exact(" ->")
  private val singleLineBlockM = Matcher.regexp("""(if|else if|else|while)\b""")
}
