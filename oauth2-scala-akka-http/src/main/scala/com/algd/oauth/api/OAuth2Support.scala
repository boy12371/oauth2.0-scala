package com.algd.oauth.api

import akka.http.marshalling.{ToEntityMarshaller, ToResponseMarshallable}
import akka.http.server.{Directive, Directive1, Route, Directives}
import JsonEntities._
import com.algd.oauth.authorizer.{BaseAuthorizer, Authorizer}
import com.algd.oauth.data.ValidationManager
import com.algd.oauth.data.model.{User, TokenResponse}
import com.algd.oauth.exception.OAuthError
import com.algd.oauth.granter.BaseGranter
import com.algd.oauth.utils.OAuthParams

import scala.concurrent.{Future, ExecutionContext}
import akka.http.marshallers.sprayjson.SprayJsonSupport._
import akka.http.server.Directives._

import scala.util.{Failure, Success}

object OAuth2Support {
  implicit class GranterRoute[T<:User](granter: BaseGranter[T])(implicit ec: ExecutionContext, vm: ValidationManager[T]) {
    def onResponse: Directive1[Either[JsonErrorResponse, JsonTokenResponse]] = {
      parameterMap.flatMap { params =>
        onComplete(granter.apply(params)).flatMap {
          case Success(response) => provide(Right(jsonTokenWithState(response, params.get(OAuthParams.STATE))))
          case Failure(error: OAuthError) => provide(Left(jsonErrorWithState(error, params.get(OAuthParams.STATE))))
          case Failure(another) => throw another
        }
      }
    }
  }

  implicit class AuthorizerRoute[T<:User, R](auth: BaseAuthorizer[T, R])(implicit ec: ExecutionContext, vm: ValidationManager[T]) {
    def onResponse(user: T): Directive1[Either[JsonErrorResponse, R]] = {
      parameterMap.flatMap { params =>
        onComplete(auth.apply(user, params)).flatMap {
          case Success(response) => null//provide(Right(jsonTokenWithState(response, params.get(OAuthParams.STATE))))
          case Failure(error: OAuthError) => provide(Left(jsonErrorWithState(error, params.get(OAuthParams.STATE))))
          case Failure(another) => throw another
        }
      }
    }
  }
}
