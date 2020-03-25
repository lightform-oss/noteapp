package com.lightform.cloud.noteapp.services

import cats.free.Free
import cats.implicits._
import cats.{Parallel, ~>}
import com.lightform.cloud.noteapp.models.Note
import com.lightform.cloud.noteapp.services.NoteService._
import doobie.ConnectionIO
import doobie.implicits._
import doobie.quill.DoobieContext

import io.getquill.{idiom => _, _}

object NoteService {
  sealed trait NoteServiceA[A] {
    def free: NoteService[A] = Free.liftF(this)
  }

  case class ListNotes(userId: String, page: Int, limit: Int)
      extends NoteServiceA[Seq[Note]]

  case class CountNotes(userId: String) extends NoteServiceA[Int]

  case class CreateNote(userId: String, title: String, body: String)
      extends NoteServiceA[Int]

  case class RetrieveNote(id: Int) extends NoteServiceA[Note]

  case class UpdateTitle(id: Int, title: String) extends NoteServiceA[Unit]

  case class UpdateBody(id: Int, body: String) extends NoteServiceA[Unit]

  case class DeleteNote(id: Int) extends NoteServiceA[Unit]

  case class ShowOwner(id: Int) extends NoteServiceA[Option[String]]

  type NoteService[A] = Free[NoteServiceA, A]

  implicit val nsPar = Parallel.identity[NoteService]

  def updateNote(id: Int, title: Option[String], body: Option[String]) =
    (
      title
        .map(UpdateTitle(id, _))
        .map(_.free)
        .getOrElse(Free.pure[NoteServiceA, Unit](())),
      body
        .map(UpdateBody(id, _))
        .map(_.free)
        .getOrElse(Free.pure[NoteServiceA, Unit](()))
    ).parMapN((_, _) => ())
}

class DbNoteServiceInterpreter extends (NoteServiceA ~> ConnectionIO) {
  val ctx = new DoobieContext.Postgres(Literal)
  import ctx._

  def apply[A](fa: NoteServiceA[A]): ConnectionIO[A] = fa match {
    case ListNotes(userId @ _, page, limit) =>
      val ret: ConnectionIO[Seq[Note]] = run(
        query[Note]
          .filter(_.username == lift(userId))
          .sortBy(_.id)
          .drop(lift((page - 1) * limit))
          .take(lift(limit))
      ).map(_.toSeq)
      ret

    case CountNotes(userId @ _) =>
      run(
        query[Note]
          .filter(_.username == lift(userId))
          .size
      ).map(_.toInt)

    case CreateNote(userId, title, body) =>
      run(
        query[Note]
          .insert(lift(Note(0, userId, title, body)))
          .returningGenerated(_.id)
      )

    case RetrieveNote(id) =>
      run(query[Note].filter(_.id == lift(id)))
        .map(_.headOption)
        .flatMap {
          case None =>
            new NoSuchElementException(s"Note $id does not exist")
              .raiseError[ConnectionIO, Note]
          case Some(n) => n.pure[ConnectionIO]
        }

    case UpdateTitle(id @ _, title) =>
      run(query[Note].filter(_.id == lift(id)).update(_.title -> lift(title)))
        .map(_ => ())

    case UpdateBody(id @ _, body @ _) =>
      run(query[Note].filter(_.id == lift(id)).update(_.body -> lift(body)))
        .map(_ => ())

    case DeleteNote(id @ _) =>
      run(query[Note].filter(_.id == lift(id)).delete).map(_ => ())

    case ShowOwner(id @ _) =>
      run(query[Note].filter(_.id == lift(id)).map(_.username))
        .map(_.headOption)
  }
}
