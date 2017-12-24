//
// Scaled Kotlin Mode - a Scaled major mode for editing Kotlin code
// http://github.com/scaled/kotlin-mode/blob/master/LICENSE

package scaled.kotlin

import java.util.function.Consumer
import org.junit.*
import org.junit.Assert.*
import scaled.*
import scaled.code.CodeConfig
import scaled.grammar.*
import scaled.impl.BufferImpl

class KotlinModeTest {

  val testCode = Std.seq(
    //                1         2         3         4         5         6         7         8
    //      012345678901234567890123456789012345678901234567890123456789012345678901234567890123456
    /* 0*/ "package foo",
    /* 1*/ "import java.util.List",
    /* 2*/ "",
    /* 3*/ "/**",
    /* 4*/ " * This is some test Kolin code that we'll use to test {@link Grammar} and specifically",
    /* 5*/ " * the {@literal Kotlin} grammar.",
    /* 6*/ " * @see http://manual.macromates.com/en/language_grammars",
    /* 7*/ " */",
    /* 8*/ "class Test (val pants :String) : Baffle() {",
    /* 9*/ "   /**",
    /*10*/ "    * A constructor, <b>woo</b>!",
    /*11*/ "    * @param foo for fooing.",
    /*12*/ "    */",
    /*13*/ "   constructor () : this(\"Jeans\")",
    /*14*/ "",
    /*15*/ "   /**",
    /*16*/ "    * A method. How exciting. Let's {@link Test} to something.",
    /*17*/ "    * @throws IllegalArgumentException if we feel like it.",
    /*18*/ "    */",
    /*19*/ "   @Deprecated(\"Use peanuts\")",
    /*20*/ "   fun test (count :Int) {}",
    /*21*/ "}").mkString("\n")

  fun rsrc (path :String) = KotlinMode::class.java.getClassLoader().getResource(path)
  fun kotlin () = rsrc("Kotlin.ndf")
  val grammars = Std.seq(Grammar.parseNDF(kotlin()))

  // @Test fun dumpGrammar () {
  //   Grammar.parseNDF(kotlin()).print(System.out)
  // }

  @Test fun testStylesLink () {
    val buffer = BufferImpl.apply(TextStore("Test.kt", "", testCode))
    val scoper = Grammar.testScoper(
      grammars, buffer, Std.list(Selector.Processor(KotlinGrammarPlugin().effacers())))
    // println(scoper.showMatchers(Set("#code", "#class")))

    // scoper.rethinkBuffer()
    // for (ll in 0..buffer.lines().length()-1) {
    //   println("$ll: ${buffer.line(ll)}")
    //   println("    " + buffer.line(ll).lineTags())
    //   for (s in scoper.showScopes(ll)) { println("    $s") }
    // }

    // assertTrue("@link contents scoped as link",
    //            scoper.scopesAt(Loc.apply(3, 61)).contains("markup.underline.link.javadoc"))
    // assertEquals("@link contents styled as link",
    //              Std.list(CodeConfig.preprocessorStyle()), buffer.stylesAt(Loc.apply(3, 61)))
  }

  val scratchCode = Std.seq(
    //                1         2         3         4         5         6         7         8
    //      012345678901234567890123456789012345678901234567890123456789012345678901234567890123456
    /* 0*/ "fun foo () {",
    /* 1*/ "  bar.get() // bingle",
    /* 2*/ "}").mkString("\n")

  @Test fun testScratchCode () {
    val buffer = BufferImpl.apply(TextStore("Test.kt", "", scratchCode))
    val scoper = Grammar.testScoper(
      grammars, buffer, Std.list(Selector.Processor(KotlinGrammarPlugin().effacers())))
    scoper.rethinkBuffer()
    for (ll in 0..buffer.lines().length()-1) {
      println("$ll: ${buffer.line(ll)}")
      println("    " + buffer.line(ll).lineTags())
      for (s in scoper.showScopes(ll)) { println("    $s") }
    }
    // assertTrue("@link contents scoped as link",
    //            scoper.scopesAt(Loc.apply(3, 61)).contains("markup.underline.link.javadoc"))
    // assertEquals("@link contents styled as link",
    //              Std.list(CodeConfig.preprocessorStyle()), buffer.stylesAt(Loc.apply(3, 61)))
  }
}
