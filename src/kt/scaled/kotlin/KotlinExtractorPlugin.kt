//
// Scaled Kotlin Project Support - Kotlin project support for Scaled project framework.
// http://github.com/scaled/kotlin-project/blob/master/LICENSE

package scaled.project

import codex.extract.Extractor
import codex.extract.TokenExtractor
import scaled.*

@Plugin(tag="codex-extractor")
class KotlinExtractorPlugin : ExtractorPlugin() {

  override fun suffs () = Std.set("kt")

  override fun extractor (project :Project, suff :String) = Option.some<Extractor>(TokenExtractor())
}
