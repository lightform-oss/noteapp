package com.lightform.cloud.noteapp.payloads.requests

import play.api.libs.json.Json

case class UpdateNoteRequest(title: Option[String], body: Option[String])
object UpdateNoteRequest {
  implicit val format = Json.format[UpdateNoteRequest]
}
