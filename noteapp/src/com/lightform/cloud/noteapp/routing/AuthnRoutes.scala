package com.lightform.cloud.noteapp.routing

import akka.http.scaladsl.server.Directives._
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import com.lightform.cloud.noteapp.payloads.responses._
import akka.http.scaladsl.model.StatusCodes.BadRequest
import scala.concurrent.duration._
import scala.language.postfixOps
import com.lightform.cloud.noteapp.services.AuthnService.AuthnServiceA
import com.lightform.cloud.noteapp.services.AuthnService
import cats.~>
import com.lightform.cloud.noteapp.misc._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext

class AuthnRoutes(
    implicit interpreter: AuthnServiceA ~> Future,
    ec: ExecutionContext
) extends PlayJsonSupport {
  // Format: OFF
  val routes =
    post(path("token")(
      concat(
        formFields("grant_type" ! "password", "username", "password")(
          (username, password) =>
            complete(AuthnService.login(username, password))
        ),
        formField("grant_type" ? "none")(grantType =>
          complete(
            BadRequest -> Failure(
              "unsupported_grant_type",
              Some(s"grant type $grantType not supported")
            )
          )
        ),
        toStrictEntity(2 seconds) (
          complete(
            BadRequest -> Failure("invalid_request")
          )
        )
      )
    ))
  // Format: ON
}
