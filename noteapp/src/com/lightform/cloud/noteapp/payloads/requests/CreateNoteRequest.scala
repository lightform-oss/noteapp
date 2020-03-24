package com.lightform.cloud.noteapp.payloads.requests

import play.api.libs.json.Json

case class CreateNoteRequest(title: String, body: String)
object CreateNoteRequest {
  implicit val format = Json.format[CreateNoteRequest]
}
