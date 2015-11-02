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

  val testJavaCode = Std.seq(
    //                1         2         3         4         5         6         7         8
    //      012345678901234567890123456789012345678901234567890123456789012345678901234567890123456
    /* 0*/ "package foo",
    /* 1*/ "",
    /* 2*/ "/**",
    /* 3*/ " * This is some test Kolin code that we'll use to test {@link Grammar} and specifically",
    /* 4*/ " * the {@literal Kotlin} grammar.",
    /* 5*/ " * @see http://manual.macromates.com/en/language_grammars",
    /* 6*/ " */",
    /* 7*/ "class Test (val pants :String) : Baffle() {",
    /* 8*/ "   /**",
    /* 9*/ "    * A constructor, <b>woo</b>!",
    /*10*/ "    * @param foo for fooing.",
    /*11*/ "    */",
    /*12*/ "   constructor () : this(\"Jeans\")",
    /*13*/ "",
    /*14*/ "   /**",
    /*15*/ "    * A method. How exciting. Let's {@link Test} to something.",
    /*16*/ "    * @throws IllegalArgumentException if we feel like it.",
    /*17*/ "    */",
    /*18*/ "   @Deprecated(\"Use peanuts\")",
    /*19*/ "   fun test (count :Int) {}",
    /*20*/ "}").mkString("\n")

  fun rsrc (path :String) = KotlinMode::class.java.getClassLoader().getResourceAsStream(path)
  fun kotlin () = rsrc("Kotlin.ndf")
  val grammars = Grammar.Set(Std.seq(Grammar.parseNDF(kotlin())))

  @Test fun dumpGrammar () {
    Grammar.parseNDF(kotlin()).print(System.out)
  }

  @Test fun testStylesLink () {
    val buffer = BufferImpl(TextStore("Test.java", "", testJavaCode))
    val scoper = Scoper(grammars, buffer, Std.list(Selector.Processor(KotlinMode.effacers)))
    // println(scoper.showMatchers(Set("#code", "#class")))
    for (ll in 0..buffer.lines().length()) for (s in scoper.showScopes(ll)) { println("$ll: $s") }
    assertTrue("@link contents scoped as link",
               scoper.scopesAt(Loc.apply(3, 61)).contains("markup.underline.link.javadoc"))
    assertEquals("@link contents styled as link",
                 Std.list(CodeConfig.preprocessorStyle()), buffer.stylesAt(Loc.apply(3, 61)))
  }
}
