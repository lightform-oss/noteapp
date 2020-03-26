package com.lightform.cloud.noteapp.services

import com.lightform.cloud.noteapp.payloads.responses._
import cats.free.Free
import com.lightform.cloud.noteapp.services.AuthnService.AuthnServiceA
import doobie.ConnectionIO
import cats.~>
import io.getquill.{idiom => _, _}
import com.lightform.cloud.noteapp.models.User
import doobie.quill.DoobieContext
import cats.implicits._
import AuthnService._
import black.door.jose.jwk.Jwk
import black.door.jose.jwt.Jwt
import black.door.jose.jwt.Claims
import black.door.jose.json.playjson.JsonSupport._

object AuthnService {

  sealed trait AuthnServiceA[A] {
    def free = Free.liftF(this)
  }
  type AuthnService[A] = Free[AuthnServiceA, A]

  case class GetUser(username: String) extends AuthnServiceA[Option[User]]
  case class GenerateToken(username: String) extends AuthnServiceA[String]
  case class ValidateToken(token: String) extends AuthnServiceA[Option[String]]

  def login(username: String, pass: String): AuthnService[OAuth2TokenResponse] =
    for {
      maybeUser <- GetUser(username).free
      token <- maybeUser match {
        case Some(user) if user.password == pass =>
          GenerateToken(user.name).free
            .map(Success(_))
        case _ =>
          Failure("invalid_grant").pure[AuthnService]
      }
    } yield token
}

class DbAuthnServiceInterpreter(key: Jwk)
    extends (AuthnServiceA ~> ConnectionIO) {
  val ctx = new DoobieContext.Postgres(NamingStrategy(Literal, PostgresEscape))
  import ctx._

  def apply[A](fa: AuthnServiceA[A]) = fa match {
    case GetUser(username) =>
      run(
        query[User]
          .filter(_.name == lift(username.toLowerCase))
      ).map(_.headOption)

    case GenerateToken(username) =>
      Jwt.sign(Claims(sub = Some(username)), key).pure[ConnectionIO]

    case ValidateToken(token) =>
      Jwt
        .validate(token)
        .using(key)
        .now
        .toOption
        .flatMap(_.payload.sub)
        .pure[ConnectionIO]
  }
}
