package com.lightform.cloud.noteapp.payloads.responses

import play.api.libs.json.Json
import play.api.libs.json.Writes

trait OAuth2TokenResponse

case class Success(access_token: String, token_type: String = "Bearer")
    extends OAuth2TokenResponse

object Success {
  implicit val writes = Json.writes[Success]
}

case class Failure(error: String, error_description: Option[String] = None)
    extends OAuth2TokenResponse

object Failure {
  implicit val writes = Json.writes[Failure]
}

object OAuth2TokenResponse {
  implicit val writes = Writes[OAuth2TokenResponse] {
    case s: Success => Success.writes.writes(s)
    case f: Failure => Failure.writes.writes(f)
  }
}
