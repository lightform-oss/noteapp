package com.lightform.cloud.noteapp.directives

import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.Directive0
import akka.http.scaladsl.server.Directives._

trait CorsDirectives {

  val corsPreflight: Directive0 = extractRequest.flatMap(request =>
    request.method match {
      case OPTIONS =>
        val allowHeaders = request
          .header[`Access-Control-Request-Headers`]
          .map(rh => `Access-Control-Allow-Headers`(rh.headers))

        complete(
          OK -> (List(
            `Access-Control-Allow-Methods`(
              CONNECT,
              GET,
              HEAD,
              POST,
              PATCH,
              PUT,
              DELETE,
              TRACE
            ),
            `Access-Control-Allow-Origin`.*,
            `Access-Control-Max-Age`(86400)
          ) ++ allowHeaders)
        )
      case _ => pass
    }
  )

  val corsSimple =
    mapResponseHeaders(
      _.filter(_.isNot(`Access-Control-Allow-Origin`.lowercaseName))
        .filter(_.isNot(`Access-Control-Max-Age`.lowercaseName)) ++ List(
        `Access-Control-Allow-Origin`.*,
        `Access-Control-Max-Age`(86400)
      )
    )

  val cors: Directive0 = corsPreflight.tflatMap(_ => corsSimple)
}

object CorsDirectives extends CorsDirectives
