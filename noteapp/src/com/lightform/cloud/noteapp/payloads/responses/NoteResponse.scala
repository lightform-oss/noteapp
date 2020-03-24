package com.lightform.cloud.noteapp.payloads.responses

import com.lightform.cloud.noteapp.models.Note
import play.api.libs.json.Json

case class NoteResponse(id: Int, title: String, body: String)
object NoteResponse {
  def apply(note: Note): NoteResponse =
    NoteResponse(note.id, note.title, note.body)
  implicit val format = Json.format[NoteResponse]
}
