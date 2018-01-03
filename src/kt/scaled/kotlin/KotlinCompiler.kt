//
// Scaled Kotlin Project Support - Kotlin project support for Scaled project framework.
// http://github.com/scaled/kotlin-project/blob/master/LICENSE

package scaled.project

import java.nio.file.Files
import java.nio.file.Path
import scaled.*
import scaled.pacman.Filez
import scaled.pacman.Pacman
import scaled.pacman.RepoId
import scaled.util.*

abstract class KotlinCompiler (proj :Project, val java :JavaComponent) : Compiler(proj) {

  companion object {
    // matches: "/foo/bar/baz.kt:LL:CC: some error message"
    val outputM = Matcher.regexp("""^([^:]+):(\d+):(\d+): (warning|error): (.*)""")

    /** The default version of kotlinc used if none is specified. */
    val DefaultKotlincVersion = "1.2.10"
  }

  /** Options to pass to `javac`. */
  open fun javacOpts () :SeqV<String> = Std.seq()
  /** Options to pass to `kotlinc`. */
  open fun kotlincOpts () :SeqV<String> = Std.seq()
  /** The version of the Kotlin compiler to use. */
  open fun kotlincVers () :String = DefaultKotlincVersion

  /** The module name to supply to the kotlin compiler. */
  open fun moduleName () :Option<String> = Option.none()

  val log = project().metaSvc().log()

  // override fun reset () {} // NOOP!

  override fun describeEngine () = "kotlinc"

  override fun describeOptions (bb :BufferBuilder) {
    val opts = kotlincOpts()
    bb.addKeyValue("kotlinc: ", if (opts.isEmpty()) "<none>" else opts.mkString(" "))
    bb.addKeyValue("kcvers: ", kotlincVers())
  }

  override fun compile (buffer :Buffer, file :Option<Path>) =
    compile(buffer, file, project().sources().dirs(), java.buildClasspath(), java.outputDir())

  override fun nextNote (buffer :Buffer, start :Long) :Compiler.NoteLoc {
    val ploc = buffer.findForward(outputM, start, buffer.end())
    if (ploc == Loc.None()) return Compiler.NoMoreNotes()
    else try {
      val file = project().root().path().resolve(outputM.group(1))
      val eline = outputM.group(2).toInt()-1
      val ecol = outputM.group(3).toInt()-1
      val ekind = outputM.group(4)
      val errPre = outputM.group(5).trim()
      // every line after the path with leading whitespace is part of the message
      val desc = Seq.builder<String>()
      desc.append(errPre)
      var pnext = Loc(ploc).nextStart()
      while (pnext < buffer.end() && buffer.line(pnext).indexOf(Chars.isWhitespace()) == 0) {
        desc.append(buffer.line(pnext).asString())
        pnext = Loc(pnext).nextStart()
      }
      val isErr = ekind == "error"
      val note = Compiler.Note(Store.apply(file), Loc.apply(eline, ecol), desc.build(), isErr)
      return Compiler.NoteLoc(note, pnext)
    } catch (e :Exception) {
      log.log("Error parsing error buffer", e)
      return Compiler.NoMoreNotes()
    }
  }

  /** A hook called just before we initiate compilation. */
  protected open fun willCompile () {}

  protected fun compile (buffer :Buffer, file :Option<Path>, sourceDirs :SeqV<Path>,
                         classpath :SeqV<Path>, output :Path) :Future<Any> {
    // if we're not doing an incremental recompile, clean the output dir first
    if (!file.isDefined) {
      Filez.deleteAll(java.outputDir())
      Files.createDirectories(java.outputDir())
    }

    // now call down to the project which may copy things back into the output dir
    willCompile()

    // resolve the appropriate version of kotlinc
    val kotlincId = "org.jetbrains.kotlin:kotlin-compiler:${kotlincVers()}"
    val pathSep = System.getProperty("path.separator")
    val kotlincArts = Pacman.repo.mvn.resolve(RepoId.parse(kotlincId))
    val kotlinCompilerPath = scaled.Iterable.view(kotlincArts.values).mkString(pathSep)

    // enumerate the to-be-compiled source files
    val sources = Seq.builder<String>()
    fun addSrc (p :Path) {
      if (p.getFileName().toString().endsWith(".kt")) sources.append(p.toString())
    }
    if (file.isDefined()) addSrc(file.get())
    else MoreFiles.onFiles(sourceDirs, { addSrc(it) })

    val moduleOpts = if (moduleName().isDefined()) Std.seq("-module-name", moduleName().get())
                     else Std.seq()

    val result = Promise<Any>()
    if (sources.isEmpty) result.succeed(true)
    else {
      // create our command line
      val cmd = Std.seq<String>(
        "java",
        "-cp",
        kotlinCompilerPath,
        "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler",
        "-cp",
        classpath.mkString(pathSep),
        "-d",
        output.toString()
      ).concat(kotlincOpts()).concat(moduleOpts).concat(sources)

      // fork off a java process to run the kotlin compiler
      val cmdArray = Array(cmd.size(), { cmd.apply(it) })
      SubProcess.apply(SubProcess.Config(cmdArray, scaled.Map.empty(), project().root().path()),
                       project().metaSvc().exec(), buffer, { result.succeed(it) })
    }
    return result
  }
}
