package com.lightform.cloud.noteapp

import akka.http.scaladsl.server.HttpApp
import com.lightform.cloud.noteapp.routing.NoteRoutes
import akka.http.scaladsl.server.Directives._
import com.lightform.cloud.noteapp.services.DbNoteServiceInterpreter
import cats.~>
import doobie.ConnectionIO
import doobie._
import cats.effect._
import doobie.implicits._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import cats.effect._
import doobie._
import doobie.implicits._
import java.util.concurrent.Executors
import com.typesafe.config.ConfigFactory

object App extends HttpApp with App {

  val dependencies = new Dependencies

  val routes = dependencies.routes

  startServer("0.0.0.0", 8080)
}

class Dependencies {
  val cfg = ConfigFactory.load()

  implicit val cs = IO.contextShift(implicitly[ExecutionContext])

  val xa = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver",
    cfg.getString("db.url"),
    cfg.getString("db.user"),
    cfg.getString("db.pass"),
    Blocker.liftExecutionContext(
      ExecutionContext.fromExecutor(Executors.newFixedThreadPool(32))
    )
  )

  implicit val db: ConnectionIO ~> Future = new (ConnectionIO ~> Future) {
    def apply[A](fa: ConnectionIO[A]): Future[A] =
      fa.transact(xa).unsafeToFuture()
  }

  val noteService = new DbNoteServiceInterpreter

  implicit val noteServiceRunner = noteService.andThen(db)

  val noteRoutes = new NoteRoutes

  val routes =
    concat(
      noteRoutes.routes
    )
}
