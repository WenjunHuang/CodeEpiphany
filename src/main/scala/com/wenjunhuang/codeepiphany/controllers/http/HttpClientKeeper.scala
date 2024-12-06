package com.wenjunhuang.codeepiphany.controllers.http

import cats.effect.kernel.Ref.Make
import cats.effect.kernel.Sync
import cats.effect.{ Async, Ref, Resource }
import com.intellij.util.net.*
import com.wenjunhuang.codeepiphany.utils.intellijComputeContext
import okhttp3.*
import org.http4s.Uri.Host
import org.http4s.client.Client
import cats.syntax.all.*
import org.typelevel.ci.CIString

import java.net.HttpCookie
import java.security.cert.X509Certificate
import javax.net.ssl.{ SSLContext, TrustManager, X509TrustManager }
import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*
import scala.util.boundary

type CookieJar = Map[CIString, Map[CIString, HttpCookie]]

trait HttpClientKeeper[F[_]] {
  def getClient: Resource[F, Client[F]]
  def updateCookies(host: CIString, cookies: List[HttpCookie]): F[Unit]
  def getCookiesForHost(host: CIString): F[List[HttpCookie]]
  def clearCookiesForHost(host: CIString): F[Unit]
}

object HttpClientKeeper {
  def apply[F[_]: HttpClientKeeper]: HttpClientKeeper[F] = summon[HttpClientKeeper[F]]

  def make[F[_]: Make: Async](): HttpClientKeeper[F] =
    new HttpClientKeeper[F] {
      val cookieManager: Ref[F, CookieJar] = Ref.unsafe[F, CookieJar](Map.empty[CIString, Map[CIString, HttpCookie]])

      override def clearCookiesForHost(host: CIString): F[Unit] = cookieManager.update { cookies =>
        cookies.removed(host)
      }

      override def getClient: Resource[F, Client[F]] =
        Resource.suspend(Sync[F].delay {
          val proxySettings                    = ProxySettings.getInstance()
          implicit val hk: HttpClientKeeper[F] = this
          proxySettings.getProxyConfiguration match {
            case _: ProxyConfiguration.DirectProxy =>
              OkHttpBuilder[F](defaultHttpClient).resource
            case configuration =>
              val builder = defaultHttpClient
                .newBuilder()
                .proxySelector(IdeProxySelector(ProxySettingsKt.asConfigurationProvider(proxySettings))) // IntelliJ proxy selector

              // get optional proxy credentials
              val credential = ProxyUtils.getStaticProxyCredentials(proxySettings, ProxyCredentialStoreKt.asProxyCredentialProvider(ProxyCredentialStore.getInstance()))
              if credential != null then
                val authenticator = new Authenticator {
                  override def authenticate(route: Route, response: Response): Request =
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
                }
                builder.proxyAuthenticator(authenticator)
              OkHttpBuilder[F](builder.build()).resource
          }
        })

      override def getCookiesForHost(host: CIString): F[List[HttpCookie]] =
        for {
          cookies <- cookieManager.get
        } yield cookies.getOrElse(host, Map.empty).values.toList

      override def updateCookies(host: CIString, cookies: List[HttpCookie]): F[Unit] =
        Sync[F]
          .delay(cookies.map(cookie => CIString(cookie.getName) -> cookie.getValue).toMap)
          .flatMap { cookiesByDomain =>
            cookieManager.update { cookies =>
              cookies.updated(host, cookiesByDomain)
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

    OkHttpClient
      .Builder()
      .dispatcher(Dispatcher(intellijComputeContext))
      .connectionPool(ConnectionPool())
      .connectTimeout(connectionTimeout.toMillis, java.util.concurrent.TimeUnit.MILLISECONDS)
      .writeTimeout(writeTimeout.toMillis, java.util.concurrent.TimeUnit.MILLISECONDS)
      .readTimeout(readTimeout.toMillis, java.util.concurrent.TimeUnit.MILLISECONDS)
      .sslSocketFactory(sslSocketFactory, trustAllManager)
      .hostnameVerifier((hostname, session) => true)
      .build()
  }

  private lazy val defaultHttpClient = makeDefaultHttpClient(10.seconds, 10.seconds, 10.seconds)
}
