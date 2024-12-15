package com.wenjunhuang.codeepiphany.controllers.http

import cats.effect.kernel.Ref.Make
import cats.effect.kernel.Sync
import cats.effect.{ Async, Ref, Resource }
import cats.syntax.all.*
import com.intellij.util.net.*
import com.wenjunhuang.codeepiphany.model.CodeDojo
import com.wenjunhuang.codeepiphany.utils.implicits.intellijComputeContext
import okhttp3.*
import org.http4s.client.Client
import org.typelevel.ci.CIString
import org.typelevel.log4cats.{ Logger, LoggerFactory }

import java.net.HttpCookie
import java.security.cert.X509Certificate
import javax.net.ssl.{ SSLContext, TrustManager, X509TrustManager }
import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*
import scala.util.boundary

type CookieJar = Map[CodeDojo, Map[CIString, HttpCookie]]

trait HttpClientKeeper[F[_]] {
  def getClient: Resource[F, Client[F]]
  def updateCookiesForHost(host: CIString, cookies: List[HttpCookie]): F[Unit]
  def getCookiesForHost(host: CIString): F[List[HttpCookie]]
  def clearCookiesForHost(host: CIString): F[Unit]
}

object HttpClientKeeper {
  def apply[F[_]: HttpClientKeeper]: HttpClientKeeper[F] = summon[HttpClientKeeper[F]]

  def make[F[_]: Make: Async: LoggerFactory](): HttpClientKeeper[F] =
    new HttpClientKeeper[F] {
      implicit val logger: Logger[F]       = LoggerFactory[F].getLogger
      val cookieManager: Ref[F, CookieJar] = Ref.unsafe[F, CookieJar](Map.empty[CodeDojo, Map[CIString, HttpCookie]])

      override def clearCookiesForHost(host: CIString): F[Unit] = cookieManager.update { cookies =>
        CodeDojo.fromHostname(host).fold(cookies)(cookies.removed)
      }

      override def getClient: Resource[F, Client[F]] = {
        implicit val hk: HttpClientKeeper[F] = this
        Resource.suspend(Sync[F].delay {
          OkHttpBuilder[F](defaultHttpClient).resource
        })
      }

      override def getCookiesForHost(host: CIString): F[List[HttpCookie]] =
        for {
          cookies <- cookieManager.get
        } yield CodeDojo.fromHostname(host).fold(List.empty[HttpCookie])(cookies.getOrElse(_, Map.empty).values.toList)

      override def updateCookiesForHost(host: CIString, cookies: List[HttpCookie]): F[Unit] =
        Sync[F]
          .delay(cookies.map(cookie => CIString(cookie.getName) -> cookie).toMap)
          .flatMap { cookiesByDomain =>
            cookieManager.update { cookies =>
              CodeDojo.fromHostname(host).fold(cookies)(codeDojo => cookies.updated(codeDojo, cookiesByDomain))
            }
          }
    }

  private val trustAllManager = new X509TrustManager {
    override def checkClientTrusted(x509Certificates: Array[X509Certificate], s: String): Unit = {}

    override def checkServerTrusted(x509Certificates: Array[X509Certificate], s: String): Unit = {}

    override def getAcceptedIssuers: Array[X509Certificate] = Array.empty
  }

  private def makeDefaultHttpClient(connectionTimeout: FiniteDuration, writeTimeout: FiniteDuration, readTimeout: FiniteDuration): OkHttpClient = {
    val sslContext = SSLContext.getInstance("SSL")
    sslContext.init(null, Array[TrustManager](trustAllManager), new java.security.SecureRandom())
    val sslSocketFactory = sslContext.getSocketFactory
    val proxySettings    = ProxySettings.getInstance()
    // get optional proxy credentials
    val authenticator = new Authenticator {
      override def authenticate(route: Route, response: Response): Request = {
        val credential = ProxyUtils.getStaticProxyCredentials(proxySettings, ProxyCredentialStoreKt.asProxyCredentialProvider(ProxyCredentialStore.getInstance()))
        if credential != null then
          boundary:
            for challenge <- response.challenges().asScala do
              if challenge.scheme().equalsIgnoreCase("OkHttp-Preemptive") then
                boundary.break(
                  response
                    .request()
                    .newBuilder()
                    .header("Proxy-Authorization", Credentials.basic(credential.getUserName, credential.getPasswordAsString))
                    .build()
                )
            null
        else null
      }
    }
    OkHttpClient
      .Builder()
      .dispatcher(Dispatcher(intellijComputeContext))
      .connectionPool(ConnectionPool())
      .connectTimeout(connectionTimeout.toMillis, java.util.concurrent.TimeUnit.MILLISECONDS)
      .writeTimeout(writeTimeout.toMillis, java.util.concurrent.TimeUnit.MILLISECONDS)
      .readTimeout(readTimeout.toMillis, java.util.concurrent.TimeUnit.MILLISECONDS)
      .sslSocketFactory(sslSocketFactory, trustAllManager)
      .hostnameVerifier((hostname, session) => true)
      .proxySelector(IdeProxySelector(ProxySettingsKt.asConfigurationProvider(proxySettings))) // IntelliJ proxy selector
      .proxyAuthenticator(authenticator)
      .addInterceptor(
        (chain: Interceptor.Chain) => {
          val request = chain.request()
          val requestWithUserAgent =
            request.newBuilder()
              .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0")
              .header("Accept","text/*,application/xml,application/json")
              .build()
          chain.proceed(requestWithUserAgent)
        }
      )
      .build()
  }

  private lazy val defaultHttpClient = makeDefaultHttpClient(10.seconds, 10.seconds, 10.seconds)
}
