package com.lightform.cloud.noteapp.routing

import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Directive1
import cats.implicits._
import cats.~>
import com.lightform.cloud.noteapp.misc._
import com.lightform.cloud.noteapp.payloads.requests.{
  CreateNoteRequest,
  UpdateNoteRequest
}
import com.lightform.cloud.noteapp.payloads.responses.{
  ListNotesResponse,
  NoteResponse
}
import com.lightform.cloud.noteapp.services.NoteService
import com.lightform.cloud.noteapp.services.NoteService._
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport

import scala.concurrent.{ExecutionContext, Future}

class NoteRoutes(extractUserId: Directive1[String])(
    implicit interpreter: NoteServiceA ~> Future,
    ec: ExecutionContext
) extends PlayJsonSupport {

  // Format: OFF
  val routes = concat(
    path("notes")(
      get(listNotes) ~
      post(createNote)
    ),
    path("notes" / IntNumber)(noteId =>
      get(retrieveNote(noteId)) ~
      patch(updateNote(noteId)) ~
      delete(deleteNote(noteId))
    )
  )
  // Format: ON

  lazy val listNotes =
    extractUserId(userId =>
      parameters("page".s.as[Int] ? 1, "limit".s.as[Int] ? 10)((page, limit) =>
        complete(
          for {
            total <- CountNotes(userId).free
            notes <- ListNotes(userId, page, limit).free
          } yield ListNotesResponse(
            total,
            Uri("/notes").withQuery(
              Query(
                "page" -> page.toString,
                "limit" -> limit.toString
              )
            ),
            Uri("/notes").withQuery(
              Query(
                "page" -> (page + 1).toString,
                "limit" -> limit.toString
              )
            ),
            notes.map(NoteResponse(_))
          )
        )
      )
    )

  lazy val createNote = extractUserId(userId =>
    entity(as[CreateNoteRequest])(request =>
      complete(CreateNote(userId, request.title, request.body).free)
    )
  )

  def retrieveNote(noteId: Int) =
    extractUserId(userId =>
      authorize(userId, noteId)(
        complete(RetrieveNote(noteId).free.map(NoteResponse(_)))
      )
    )

  def updateNote(noteId: Int) =
    extractUserId(userId =>
      authorize(userId, noteId)(
        entity(as[UpdateNoteRequest])(request =>
          complete(NoteService.updateNote(noteId, request.title, request.body))
        )
      )
    )

  def deleteNote(noteId: Int) =
    extractUserId(userId =>
      authorize(userId, noteId)(
        complete(DeleteNote(noteId).free)
      )
    )

  def authorize(userId: String, noteId: Int) = authorizeAsync(
    ShowOwner(noteId).free.map(_.contains(userId)).foldMap(interpreter)
  )
}
