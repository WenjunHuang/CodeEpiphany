package com.wenjunhuang.codeepiphany.services.http

import cats.effect.{IO, Ref, Resource}
import java.net
import java.net.HttpCookie
import java.security.cert.X509Certificate
import javax.net.ssl.{SSLContext, TrustManager, X509TrustManager}
import okhttp3.*
import org.http4s.client.Client
import org.typelevel.ci.CIString
import scala.annotation.static
import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*
import scala.util.boundary

import com.intellij.openapi.util.io.FileUtil

import com.wenjunhuang.codeepiphany.model.CodeDojo
import com.wenjunhuang.codeepiphany.utils.CompatibleUtils
import com.wenjunhuang.codeepiphany.utils.syntax.*

type CookieJar = Map[CodeDojo, Map[CIString, HttpCookie]]

trait HttpClientManager {
  def getClient: Resource[IO, Client[IO]]

  def updateCookiesForHost(host: CIString, cookies: List[HttpCookie]): IO[Unit]

  def getCookiesForHost(host: CIString): IO[List[HttpCookie]]

  def findCookieForHost(host: CIString, cookieName: CIString): IO[Option[HttpCookie]]

  def clearCookiesForHost(host: CIString): IO[Unit]
}

object HttpClientManager extends HttpClientManager {

  private val cookieManager: Ref[IO, CookieJar] =
    Ref.unsafe[IO, CookieJar](Map.empty[CodeDojo, Map[CIString, HttpCookie]])

  override def clearCookiesForHost(host: CIString): IO[Unit] = cookieManager.update { cookies =>
    CodeDojo.fromCIHostname(host).fold(cookies)(cookies.removed)
  }

  override def getClient: Resource[IO, Client[IO]] = OkHttpBuilder.fromUnmanaged(defaultHttpClient).resource

  override def findCookieForHost(host: CIString, cookieName: CIString): IO[Option[HttpCookie]] =
    getCookiesForHost(host).map(_.find(cookie => CIString(cookie.getName) == cookieName))

  override def getCookiesForHost(host: CIString): IO[List[HttpCookie]] =
    for {
      cookies <- cookieManager.get
    } yield CodeDojo
      .fromCIHostname(host)
      .fold(List.empty[HttpCookie])(cookies.getOrElse(_, Map.empty).values.toList)

  override def updateCookiesForHost(host: CIString, cookies: List[HttpCookie]): IO[Unit] =
    IO
      .delay(cookies.map(cookie => CIString(cookie.getName) -> cookie).toMap)
      .flatMap { cookiesByDomain =>
        cookieManager.update { cookies =>
          CodeDojo
            .fromCIHostname(host)
            .fold(cookies) { codeDojo =>
              cookies.updatedWith(codeDojo) {
                case None => Some(cookiesByDomain)
                case Some(exists) =>
                  Some(exists ++ cookiesByDomain)
              }
            }
        }
      }

  private val trustAllManager = new X509TrustManager {
    override def checkClientTrusted(x509Certificates: Array[X509Certificate], s: String): Unit = {}

    override def checkServerTrusted(x509Certificates: Array[X509Certificate], s: String): Unit = {}

    override def getAcceptedIssuers: Array[X509Certificate] = Array.empty
  }

  private def makeDefaultHttpClient(
    connectionTimeout: FiniteDuration,
    writeTimeout: FiniteDuration,
    readTimeout: FiniteDuration
  ): OkHttpClient = {
    java.util.logging.Logger.getLogger(classOf[OkHttpClient].getName).setLevel(java.util.logging.Level.FINE)
    val sslContext = SSLContext.getInstance("SSL")
    sslContext.init(null, Array[TrustManager](trustAllManager), new java.security.SecureRandom())
    val sslSocketFactory = sslContext.getSocketFactory
    // get optional proxy credentials
    // reference: https://square.github.io/okhttp/3.x/okhttp/okhttp3/Authenticator.html

    OkHttpClient
      .Builder()
      .dispatcher(Dispatcher(intellijComputeContext))
      .followRedirects(false)
      .followSslRedirects(false)
      .cache(Cache(FileUtil.createTempDirectory("ce", "okhttp", true), 10L * 1024L * 1024L))
      .connectTimeout(connectionTimeout.toMillis, java.util.concurrent.TimeUnit.MILLISECONDS)
      .writeTimeout(writeTimeout.toMillis, java.util.concurrent.TimeUnit.MILLISECONDS)
      .readTimeout(readTimeout.toMillis, java.util.concurrent.TimeUnit.MILLISECONDS)
      .sslSocketFactory(sslSocketFactory, trustAllManager)
      .hostnameVerifier((_, _) => true)
      .proxySelector(CompatibleUtils.getIdeaProxySelector)
      .proxyAuthenticator((route: Route, response: Response) => {
        CompatibleUtils.getIdeaProxyPasswordAuthentication(response.request().url().url()) match
          case null => null
          case authentication =>
            boundary:
              for challenge <- response.challenges().asScala do
                if challenge.scheme().equalsIgnoreCase("OkHttp-Preemptive") then
                  boundary.break(
                    response
                      .request()
                      .newBuilder()
                      .header(
                        "Proxy-Authorization",
                        Credentials.basic(authentication.getUserName, String(authentication.getPassword))
                      )
                      .build()
                  )
              null
      })
      .addInterceptor((chain: Interceptor.Chain) => {
        chain.proceed(
          chain
            .request()
            .newBuilder()
            .header(
              "User-Agent",
              "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0"
            )
            .build()
        )
      })
      .build()
  }

  private lazy val defaultHttpClient = makeDefaultHttpClient(10.seconds, 10.seconds, 10.seconds)

  @static
  def getHttpClient: OkHttpClient = defaultHttpClient
}
