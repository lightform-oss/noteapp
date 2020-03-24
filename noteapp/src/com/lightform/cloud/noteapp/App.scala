package com.lightform.cloud.noteapp

import akka.http.scaladsl.server.HttpApp
import com.lightform.cloud.noteapp.routing.NoteRoutes
import akka.http.scaladsl.server.Directives._
import com.lightform.cloud.noteapp.services.DbNoteServiceInterpreter
import cats.~>
import doobie.ConnectionIO
import doobie._
import cats.effect._
import doobie.hikari._
import doobie.implicits._
import scala.language.reflectiveCalls
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import cats.effect._
import doobie._
import doobie.implicits._
import doobie.hikari._

object App extends HttpApp with App {

  val dependencies = new Dependencies

  val resources = dependencies.transactor

  val routes =
    dependencies.transactor
      .use(xa => IO.pure(dependencies.withTx(xa).routes))
      .unsafeRunSync

  startServer("0.0.0.0", 8080)
}

class Dependencies {
  implicit val cs = IO.contextShift(implicitly[ExecutionContext])

  val transactor: Resource[IO, HikariTransactor[IO]] = for {
    ce <- ExecutionContexts.fixedThreadPool[IO](32)
    be <- Blocker[IO]
    xa <- HikariTransactor
      .newHikariTransactor[IO](???, ???, ???, ???, ce, be)
  } yield xa

  val withTx = (xa: Transactor[IO]) =>
    new Object {

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
}
