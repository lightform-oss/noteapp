package com.lightform.cloud.noteapp

import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.Credentials.Provided
import scala.concurrent.Future

package object directives {

  def extractUserId(
      validate: String => Future[Option[String]]
  ): Directive1[String] =
    authenticateOAuth2Async("noteapp", {
      case Provided(token) => validate(token)
      case _               => Future.successful(None)
    })
}
