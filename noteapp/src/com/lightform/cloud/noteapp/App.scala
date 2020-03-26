package com.lightform.cloud.noteapp

import com.lightform.cloud.noteapp.routing.NoteRoutes
import akka.http.scaladsl.server.Directives._
import com.lightform.cloud.noteapp.services.DbNoteServiceInterpreter
import cats.~>
import doobie.ConnectionIO
import doobie._
import cats.effect._
import doobie.implicits._
import scala.concurrent.{ExecutionContext, Future}
import cats.effect._
import doobie._
import doobie.implicits._
import java.util.concurrent.Executors
import com.typesafe.config.ConfigFactory
import com.lightform.cloud.noteapp.directives.CorsDirectives._
import play.api.libs.json.Json
import black.door.jose.jwk.Jwk
import black.door.jose.json.playjson.JsonSupport._
import com.lightform.cloud.noteapp.services.DbAuthnServiceInterpreter
import com.lightform.cloud.noteapp.routing.AuthnRoutes
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directive1
import cats.implicits._
import com.lightform.cloud.noteapp.services.AuthnService.ValidateToken
import com.lightform.cloud.noteapp.directives.extractUserId
import akka.http.scaladsl.server.Route

object App extends App {
  implicit val sys = ActorSystem()
  implicit val ec = sys.dispatcher

  val dependencies = new Dependencies

  Http()
    .bindAndHandle(
      dependencies.routes(provide("anon")),
      "0.0.0.0",
      8080
    )
    .onComplete(o => println(o))

  Http()
    .bindAndHandle(
      dependencies.routes(dependencies.authn),
      "0.0.0.0",
      8888
    )
    .onComplete(o => println(o))
}

class Dependencies(implicit ec: ExecutionContext) {
  val cfg = ConfigFactory.load()

  implicit val cs = IO.contextShift(implicitly[ExecutionContext])

  val xa = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver",
    cfg.getString("NoteApp.db.url"),
    cfg.getString("NoteApp.db.user"),
    cfg.getString("NoteApp.db.pass"),
    Blocker.liftExecutionContext(
      ExecutionContext.fromExecutor(Executors.newFixedThreadPool(32))
    )
  )

  implicit val db: ConnectionIO ~> Future = new (ConnectionIO ~> Future) {
    def apply[A](fa: ConnectionIO[A]): Future[A] =
      fa.transact(xa).unsafeToFuture()
  }

  val tokenKey = Json
    .parse(
      cfg.getString("NoteApp.key")
    )
    .as[Jwk]
    
  val authnService = new DbAuthnServiceInterpreter(tokenKey)
  implicit val authnServiceRunner = authnService.andThen(db)

  val noteService = new DbNoteServiceInterpreter
  implicit val noteServiceRunner = noteService.andThen(db)

  val authnRoutes = new AuthnRoutes

  val authn = extractUserId(token =>
    ValidateToken(token).free.foldMap(authnServiceRunner)
  )

  def routes(authn: Directive1[String]) =
    cors(
      Route.seal(
        concat(
          new NoteRoutes(authn).routes,
          authnRoutes.routes
        )
      )
    )
}
