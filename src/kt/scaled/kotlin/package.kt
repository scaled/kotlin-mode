//
// Scaled Kotlin Mode - a Scaled major mode for editing Kotlin code
// http://github.com/scaled/kotlin-mode/blob/master/LICENSE

package scaled.kotlin

inline fun <T,R> sfun (crossinline fn :(T) -> R) :scala.Function1<T,R> =
  object : scala.runtime.AbstractFunction1<T,R>() {
    override fun apply (arg :T) :R = fn(arg)
  }
