package com.lightform.cloud.noteapp

import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives.provide

package object directives {
  val extractUserId: Directive1[String] = provide("anon")
}
