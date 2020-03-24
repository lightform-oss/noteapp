package com.lightform.cloud.noteapp.payloads.responses

import akka.http.scaladsl.model.Uri
import com.lightform.cloud.noteapp.misc.uriFormat
import play.api.libs.json.Json

object ListNotesResponseLink {
  implicit val format = Json.format[ListNotesResponseLink]
}
object ListNotesResponseLinks {
  implicit val format = Json.format[ListNotesResponseLinks]
}
object ListNotesResponseEmbeds {
  implicit val format = Json.format[ListNotesResponseEmbeds]
}

case class ListNotesResponse(
    total: Int,
    _links: ListNotesResponseLinks,
    _embedded: ListNotesResponseEmbeds
)

object ListNotesResponse {
  def apply(
      total: Int,
      href: Uri,
      next: Uri,
      notes: Seq[NoteResponse]
  ): ListNotesResponse =
    ListNotesResponse(
      total,
      ListNotesResponseLinks(ListNotesResponseLink(href, next)),
      ListNotesResponseEmbeds(notes)
    )

  implicit val format = Json.format[ListNotesResponse]
}

case class ListNotesResponseLinks(self: ListNotesResponseLink)

case class ListNotesResponseLink(href: Uri, next: Uri)

case class ListNotesResponseEmbeds(notes: Seq[NoteResponse])
