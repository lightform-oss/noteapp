package com.lightform.cloud.noteapp

import akka.http.scaladsl.marshalling.{
  PredefinedToResponseMarshallers,
  ToResponseMarshaller
}
import akka.http.scaladsl.model.{StatusCodes, Uri}
import cats.arrow.FunctionK
import cats.free.Free
import cats.{Applicative, Monad, Parallel, ~>}

import scala.concurrent.{ExecutionContext, Future}
import cats.implicits._
import play.api.libs.json.{Format, JsString, Reads, Writes}

package object misc {
  type ToFuture[F[_]] = F ~> Future

  implicit class StringSyntax(val str: String) extends AnyVal {
    def s = Symbol(str)
  }

  trait FreeAuxy[S[_]] {
    type F[A] = Free[S, A]
  }

  object FreeAuxy {
    implicit def freeAuxy[S[_]]: FreeAuxy[S] = new FreeAuxy[S] {}
  }

  implicit def freeToFuture[S[_]](
      implicit interpreter: S ~> Future,
      ec: ExecutionContext,
      aux: FreeAuxy[S]
  ): aux.F ~> Future = {
    new (aux.F ~> Future) {
      def apply[A](fa: Free[S, A]): Future[A] = fa.foldMap(interpreter)
    }
  }

  implicit def higherToResponseMarshaller[S[_]: ToFuture, A: ToResponseMarshaller]
      : ToResponseMarshaller[S[A]] =
    implicitly[ToResponseMarshaller[Future[A]]]
      .compose(implicitly[S ~> Future].apply(_))

  implicit val uriFormat = Format[Uri](
    Reads(_.validate[String].map(Uri(_))),
    Writes(uri => JsString(uri.toString))
  )

  implicit val unitTRM =
    PredefinedToResponseMarshallers.fromStatusCode.compose[Unit](_ =>
      StatusCodes.NoContent
    )

  implicit def freePar[S[_]](implicit aux: FreeAuxy[S]) =
    new Parallel[aux.F] {
      type F[A] = aux.F[A]

      val applicative = Applicative[aux.F]

      val monad = Monad[aux.F]

      val sequential = FunctionK.id[aux.F]

      val parallel = FunctionK.id[aux.F]
    }
}
