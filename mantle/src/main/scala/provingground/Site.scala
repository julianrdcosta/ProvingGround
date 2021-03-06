package provingground.interface

import ammonite.ops, ops._
import Tuts._

import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer

import scala.xml.Elem

object Site{
  implicit val wd = pwd


  def pack() = {
    pprint.log("packing client")
    %%("mill", "client.pack")
  }


  def mkDocs() = {
    pprint.log("generating scaladocs")
    %%("mill", "jvmRoot.docs")
  }

  def assemble() = {
    pprint.log("assembling mantle")
    %%("mill", "mantle.assembly")
  }

  val mathjax =
    """
      |<!-- mathjax config similar to math.stackexchange -->
      |<script type="text/x-mathjax-config">
      |MathJax.Hub.Config({
      |jax: ["input/TeX", "output/HTML-CSS"],
      |tex2jax: {
      |  inlineMath: [ ['$', '$'] ],
      |  displayMath: [ ['$$', '$$']],
      |  processEscapes: true,
      |  skipTags: ['script', 'noscript', 'style', 'textarea', 'pre', 'code']
      |},
      |messageStyle: "none",
      |"HTML-CSS": { preferredFont: "TeX", availableFonts: ["STIX","TeX"] }
      |});
      |</script>
      |<script type="text/javascript" async
      |      src="https://cdnjs.cloudflare.com/ajax/libs/mathjax/2.7.1/MathJax.js?config=TeX-MML-AM_CHTML">
      |       </script>
    """.stripMargin

  def head(relDocsPath: String = ""): String =
    s"""
       |<head>
       |    <meta charset="utf-8">
       |    <meta http-equiv="X-UA-Compatible" content="IE=edge">
       |    <meta name="viewport" content="width=device-width, initial-scale=1">
       |    <!-- The above 3 meta tags *must* come first in the head; any other head content must come *after* these tags -->
       |    <title>ProvingGround</title>
       |    <link rel="icon" href="${relDocsPath}IIScLogo.jpg">
       |
       |    <!-- Bootstrap -->
       |    <link href="${relDocsPath}css/bootstrap.min.css" rel="stylesheet">
       |   <link href="${relDocsPath}css/katex.min.css" rel="stylesheet">
       |   <link href="${relDocsPath}css/main.css" rel="stylesheet">
       |
       |
       |    <link rel="stylesheet" href="${relDocsPath}css/zenburn.css">
       |    <script src="${relDocsPath}js/highlight.pack.js"></script>
       |    <script>hljs.initHighlightingOnLoad();</script>
       |
       |   <script src="${relDocsPath}js/ace.js"></script>
       |   <script src="${relDocsPath}js/katex.min.js"></script>
       |
       |    $mathjax
       |  </head>
       |
   """.stripMargin

  def nav(relDocsPath: String = ""): Elem =
    <nav class="navbar navbar-default">
      <div class="container-fluid">
        <!-- Brand and toggle get grouped for better mobile display -->
        <div class="navbar-header">
          <button type="button" class="navbar-toggle collapsed" data-toggle="collapse" data-target="#bs-example-navbar-collapse-1" aria-expanded="false">
            <span class="sr-only">Toggle navigation</span>
            <span class="icon-bar"></span>
            <span class="icon-bar"></span>
            <span class="icon-bar"></span>
          </button>
          <span class="navbar-brand">ProvingGround</span>
        </div>

        <!-- Collect the nav links, forms, and other content for toggling -->
        <div class="collapse navbar-collapse" id="bs-example-navbar-collapse-1">
          <ul class="nav navbar-nav" id="left-nav">
            <li><a href={s"${relDocsPath}index.html"}>Docs Home</a></li>
            <li class="dropdown">
              <a href="#" class="dropdown-toggle" data-toggle="dropdown" role="button" aria-haspopup="true" aria-expanded="false">
                Tutorials (notes)<span class="caret"></span></a>
              <ul class="dropdown-menu">
                {tutList(relDocsPath)}
              </ul>
            </li>
          </ul>
          <ul class="nav navbar-nav navbar-right">
            <li> <a href={s"${relDocsPath}scaladoc/provingground/index.html"} target="_blank">ScalaDocs</a></li>
            <li> <a href="https://github.com/siddhartha-gadgil/ProvingGround" target="_blank">
              <img src={s"${relDocsPath}GitHub-Mark-Light-32px.png"} alt="Github"></img> </a> </li>



          </ul>
        </div><!-- /.navbar-collapse -->
      </div><!-- /.container-fluid -->
    </nav>

  def foot(relDocsPath: String): String =
    s"""
       |<div class="container-fluid">
       |  <p>&nbsp;</p>
       |  <p>&nbsp;</p>
       |  <p>&nbsp;</p>
       |  <div class="footer navbar-fixed-bottom bg-primary">
       |    <h4>
       |    &nbsp;Developed by:
       |    &nbsp;<a href="http://math.iisc.ac.in/~gadgil" target="_blank">&nbsp; Siddhartha Gadgil</a>
       |
       |  </h4>
       |
       |  </div>
       |</div>
       |<script type="text/javascript" src="${relDocsPath}js/jquery-2.1.4.min.js"></script>
       |<script type="text/javascript" src="${relDocsPath}js/bootstrap.min.js"></script>
       |<script type="text/javascript" src="${relDocsPath}js/provingground.js"></script>
       |<script>
       |  provingground.main()
       |</script>
   """.stripMargin



  def fromMD(s: String) : String = {
    val parser = Parser.builder().build()
    val document = parser.parse(s)
    val renderer = HtmlRenderer.builder().build()
    renderer.render(document).replace("$<code>", "$").replace("</code>$", "$")
  }


  def threeDash(s: String) = s.trim == "---"

  def withTop(l: Vector[String]) = (l.filter(threeDash).size == 2) && threeDash(l.head)

  def body(l: Vector[String]) = if (withTop(l)) l.tail.dropWhile((l) => !threeDash(l)).tail else l

  def topmatter(lines: Vector[String]) = if (withTop(lines))  Some(lines.tail.takeWhile((l) => !threeDash(l))) else None

  def titleOpt(l: Vector[String]) =
    for {
      tm <- topmatter(l)
      ln <- tm.find(_.startsWith("title: "))
    } yield ln.drop(6).trim

  def filename(s: String) = s.toLowerCase.replaceAll("\\s", "-")

  case class Tut(name: String, rawContent: String, optTitle: Option[String]){
    val title = optTitle.getOrElse(name)

    val target = pwd / "docs" / "tuts"/ s"$name.html"

    def url(relDocsPath: String) = s"${relDocsPath}tuts/$name.html"

    def content: String = fromMD(mkTut(rawContent))

    def output: String =
      page(
        content,
        "../",
        title)

    def save = write.over(target, output)
  }

  def getTut(p: Path): Tut =
  {
    val l = ops.read.lines(p).toVector
    val name = titleOpt(l).map(filename).getOrElse(p.name.dropRight(p.ext.length + 1))
    val rawContent = body(l).mkString("\n")
    Tut(name, rawContent, titleOpt(l))
  }

  lazy val allTuts: Seq[Tut] = ls(tutdir).map(getTut)

  def tutList(relDocsPath: String): Seq[Elem] =
    allTuts.map(
      (tut) =>
        <li><a href={s"${tut.url(relDocsPath)}"}>{tut.title}</a></li>
    )


  def page(s: String, relDocsPath: String, t: String = ""): String =
    s"""
       |<!DOCTYPE html>
       |<html lang="en">
       |${head(relDocsPath)}
       |<body>
       |${nav(relDocsPath)}
       |<div class="container">
       |<h1 class="text-center">$t</h1>
       |
       |<div class="text-justify">
       |$s
       |
       |</div>
       |</div>
       |${foot(relDocsPath)}
       |</body>
       |</html>
   """.stripMargin

  val home = page(
    fromMD(body(ops.read.lines(pwd / "docs" /"index.md").toVector).mkString("", "\n", "")), "", "ProvingGround: Automated Theorem proving by learning")

  def mkSite() = {
    println("writing site")

    pack()

    mkDocs()

    assemble()

    write.over(pwd / "docs" / "index.html", home)

    allTuts.foreach{(tut) =>
      pprint.log(s"compiling tutorial ${tut.name}")
      write.over(tut.target, tut.output)
    }
  }

}
