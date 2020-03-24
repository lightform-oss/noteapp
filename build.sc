import mill._, scalalib._

object noteapp extends ScalaModule {
  def scalaVersion = "2.13.1"

  val catsV = "2.1.1"
  val joseV = "0.3.0"
  val doobieV = "0.8.8"

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
    ivy"org.postgresql:postgresql:42.2.11",
    ivy"org.tpolecat::doobie-hikari:$doobieV",
    ivy"org.tpolecat::doobie-quill:$doobieV"
  )

  def scalacOptions = Seq(
    "-Xfatal-warnings", "-feature", "-unchecked", "-deprecation",
    "-Ywarn-macros:after", "-Ywarn-unused"
  )
}
