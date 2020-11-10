import com.typesafe.sbt.site.util.SiteHelpers
import Docs._

lazy val tutorialSubDirName = settingKey[String]("subdir name for old tutorial")
lazy val fileEncoding = settingKey[String]("check the file encoding")

ThisBuild / organization := "org.scala-sbt"
ThisBuild / scalafmtOnCompile := true

lazy val root = (project in file("."))
  .enablePlugins(
    (if (!isBetaBranch) Seq(ParadoxSitePlugin) else Seq()) ++
      Seq(LowTechSnippetPamfletPlugin, ScriptedPlugin): _*
  )
  .settings(
    name := "website",
    siteEmail := "eed3si9n" + "@gmail.com",
    // Landing
    Compile / paradox / sourceDirectory := baseDirectory.value / "src" / "landing",
    Compile / paradoxTheme / sourceDirectory := (Compile / paradox / sourceDirectory).value / "_template",
    paradoxProperties ++= Map(
      "sbtVersion" -> Docs.targetSbtFullVersion,
      "windowsBuild" -> Docs.sbtWindowsBuild,
      "sbtVersionForScalaDoc" -> Docs.sbtVersionForScalaDoc,
    ),
    Compile / paradoxRoots := List(
      "404.html",
      "community.html",
      "cookie.html",
      "documentation.html",
      "download.html",
      "index.html",
      "learn.html",
      "support.html",
    ),
    // Reference
    sourceDirectory in Pamflet := baseDirectory.value / "src" / "reference",
    siteSubdirName in Pamflet := s"""$targetSbtBinaryVersion/docs""",
    tutorialSubDirName := s"""$targetSbtBinaryVersion/tutorial""",
    // Redirects
    redirectSettings,
    SiteHelpers.addMappingsToSiteDir(mappings in Redirect, siteSubdirName in Pamflet),
    redirectTutorialSettings,
    SiteHelpers.addMappingsToSiteDir(mappings in RedirectTutorial, tutorialSubDirName),
    // GitHub Pages. See project/Docs.scala
    customGhPagesSettings,
    mappings in Pamflet := {
      val xs = (mappings in Pamflet).value
      Pdf.cleanupCombinedPages(xs) ++ xs
    },
    if (scala.sys.BooleanProp.keyExists("sbt.website.generate_pdf"))
      Def settings (
        // NOTE - PDF settings must be done externally like this because pdf generation generically looks
        // through `mappings in Config` for Combined+Pages.md to generate PDF from, and therefore we
        // can't create a circular dependency by adding it back into the original mappings.
        Pdf.settings,
        Pdf.settingsFor(Pamflet, "sbt-reference"),
        SiteHelpers.addMappingsToSiteDir(
          mappings in Pdf.generatePdf in Pamflet,
          siteSubdirName in Pamflet,
        )
      )
    else if (scala.sys.BooleanProp.keyExists("sbt.website.detect_pdf"))
      Def.settings(
        // assume PDF files were created in another Docker container
        Pdf.detectPdf in Pamflet := ((target in Pamflet).value ** "*.pdf").get,
        mappings in Pdf.detectPdf in Pamflet := {
          (Pdf.detectPdf in Pamflet).value pair Path.relativeTo((target in Pamflet).value)
        },
        SiteHelpers.addMappingsToSiteDir(
          mappings in Pdf.detectPdf in Pamflet,
          siteSubdirName in Pamflet,
        )
      )
    else Nil,
    fileEncoding := {
      sys.props("file.encoding") match {
        case "UTF-8" => "UTF-8"
        case x       => sys.error(s"Unexpected encoding $x")
      }
    },
    scriptedLaunchOpts := {
      scriptedLaunchOpts.value ++
        Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
    },
    scriptedBufferLog := false,
    isGenerateSiteMap := true
  )
