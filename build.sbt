import com.typesafe.sbt.SbtGit.GitKeys

enablePlugins(GitVersioning)

ThisBuild / scalaVersion      := "2.13.2"

ThisBuild / isSnapshot        := false
ThisBuild / git.baseVersion   := "0.2-dev"
ThisBuild / git.gitHeadCommit := GitKeys.gitReader.value.withGit(
                                   _.asInstanceOf[com.typesafe.sbt.git.JGit]
                                    .headCommit.map(_.abbreviate(8).name)
                                 )

ThisBuild / organization := "com.arkondata"

ThisBuild / homepage   := Some(url("https://github.com/Grupo-Abraxas/slothql"))
ThisBuild / scmInfo    := Some(ScmInfo(homepage.value.get, "git@github.com:Grupo-Abraxas/slothql.git"))
ThisBuild / developers := List(
                            Developer("fehu", "Dmitry K", "kdn.kovalev@gmail.com", url("https://github.com/fehu"))
                          )
ThisBuild / licenses += ("MIT", url("https://opensource.org/licenses/MIT"))


lazy val root = (project in file(".")).
  settings(
    docSettings,
    inThisBuild(List(
      git.gitUncommittedChanges := (git.gitUncommittedChanges.value || isSnapshot.value),
      scalacOptions in Compile ++= Seq("-unchecked", "-feature", "-deprecation"),
      addCompilerPlugin(Dependencies.Plugin.`kind-projector`)
    ) ++ versionWithGit),

    crossScalaVersions := Nil,
    skip in publish := true,
    name := "slothql"
  )
  .aggregate(cypher, apoc, opentracingNeo4j)

lazy val cypher = (project in file("cypher"))
  .settings(
    docSettings,
    name := "slothql-cypher",
    libraryDependencies ++= Seq(
      Dependencies.Scala.reflect.value,
      Dependencies.shapeless,
      Dependencies.`cats-core`,
      Dependencies.`cats-free`,
      Dependencies.`cats-effect`,
      Dependencies.`fs2-core`,
      Dependencies.`neo4j-driver`,
      Dependencies.Test.scalatest
    ),
    initialCommands in console :=
      """
        |import org.neo4j.driver.{ AuthTokens, GraphDatabase }
        |import com.arkondata.slothql.cypher.syntax._
        |import com.arkondata.slothql.cypher.CypherFragment
        |import com.arkondata.slothql.neo4j.Neo4jCypherTransactor
      """.stripMargin
  )

lazy val apoc = (project in file("cypher-apoc"))
  .settings(
    docSettings,
    name := "slothql-cypher-apoc"
  ).dependsOn(cypher % "compile -> compile; test -> test")

lazy val opentracingNeo4j = (project in file("opentracing-neo4j"))
  .settings(
    docSettings,
    name := "slothql-opentracing-neo4j",
    scalacOptions in Compile += "-Ymacro-annotations",
    libraryDependencies ++= Seq(
      Dependencies.`opentracing-effect`,
      Dependencies.`opentracing-fs2`
    )
  ).dependsOn(cypher)

// // // Scaladoc // // //

lazy val isGraphvizPresent = {
  import scala.sys.process._
  try "dot -V".! == 0
  catch { case _: Throwable => false }
}

lazy val docSettings = Seq(
  Compile / doc / scalacOptions ++= {
    val default  = Seq("-implicits")
    val diagrams = if (isGraphvizPresent) Seq("-diagrams", "-diagrams-debug") else Seq()
    default ++ diagrams
  }
)

// Publishing

ThisBuild / publishMavenStyle := true
ThisBuild / publishTo := sonatypePublishToBundle.value

// Fix for gpg 2.2.x
// See [[https://github.com/sbt/sbt-pgp/issues/173]]
Global / PgpKeys.gpgCommand := (baseDirectory.value / "gpg.sh").getAbsolutePath

ThisBuild / credentials += Credentials(
  "Sonatype Nexus Repository Manager",
  "oss.sonatype.org",
  sys.env.getOrElse("SONATYPE_USER", ""),
  sys.env.getOrElse("SONATYPE_PWD", "")
)

// Fix for error `java.net.ProtocolException: Too many follow-up requests: 21`
// See [[https://github.com/sbt/sbt-pgp/issues/150]]
ThisBuild / updateOptions := updateOptions.value.withGigahorse(false)
