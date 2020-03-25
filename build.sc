import mill._, scalalib._

import $ivy.`com.lihaoyi::mill-contrib-flyway:$MILL_VERSION`
import mill.contrib.flyway.FlywayModule

import $ivy.`com.lihaoyi::mill-contrib-docker:$MILL_VERSION`
import contrib.docker.DockerModule

val configDep = ivy"com.typesafe:config:1.4.0"

@

import configDep.dep
import com.typesafe.config.ConfigFactory

import scala.collection.JavaConverters._

object noteapp extends ScalaModule with FlywayModule with DockerModule {
  def scalaVersion = "2.13.1"

  val catsV = "2.1.1"
  val joseV = "0.3.0"
  val doobieV = "0.8.8"

  val postgresDriverDep = ivy"org.postgresql:postgresql:42.2.11"

  def ivyDeps = Agg(
    ivy"com.typesafe.akka::akka-http:10.1.11",
    ivy"com.typesafe.play::play-json:2.8.1",
    ivy"de.heikoseeberger::akka-http-play-json:1.31.0",
    ivy"org.typelevel::cats-core:$catsV",
    ivy"org.typelevel::cats-free:$catsV",
    ivy"org.typelevel::cats-effect:$catsV",
    ivy"ch.qos.logback:logback-classic:1.2.3",
    ivy"com.typesafe.scala-logging::scala-logging:3.9.2",
    ivy"com.typesafe:config:1.4.0",
    ivy"black.door::jose:$joseV",
    ivy"black.door::jose-json-play:$joseV",
    postgresDriverDep,
    ivy"org.tpolecat::doobie-hikari:$doobieV",
    ivy"org.tpolecat::doobie-quill:$doobieV",
    configDep
  )

  def scalacOptions = Seq(
    "-Xfatal-warnings",
    "-feature",
    "-unchecked",
    "-deprecation",
    "-Ywarn-macros:after",
    "-Ywarn-unused"
  )

  def flywayDriverDeps = Agg(postgresDriverDep)
  def flywayUrl = T(dbConfig().getString("url"))//"jdbc:postgresql://localhost:4533/postgres"
  def flywayUser = T(dbConfig().getString("user"))
  def flywayPassword = T.input(dbConfig().getString("pass"))

  object docker extends DockerConfig {
    def baseImage = "gcr.io/distroless/java:11"
    def pullBaseImage = true

    def repo = T.input(T.ctx().env("DOCKER_REPO"))
    def tag = T.input(T.ctx().env("DOCKER_TAG"))

    def tags = List(s"${repo()}:${tag()}")
  }

  import upickle.default._
  def w[T] = writer[Unit].comap[T](_ => ())
  def r[T] = reader[Unit].map[T](_ => ???)
  def rw[T]: ReadWriter[T] = ReadWriter.join(r, w)
  def dbConfig = T.input {
    ConfigFactory.load(ConfigFactory.parseFile((resources().head.path / "application.conf").toIO)
        // this task evaluates in a long running process, so we need to sideload the environment variables into the config
        .withFallback(ConfigFactory.parseMap(T.ctx().env.asJava))).getConfig("db")
  }(rw, implicitly[mill.define.Ctx])

}
