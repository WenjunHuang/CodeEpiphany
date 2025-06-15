package com.wenjunhuang.codeepiphany.services.http

import cats.effect.kernel.Ref.Make
import cats.effect.kernel.Sync
import cats.effect.{ Async, Ref, Resource }
import cats.syntax.all.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.net.*
import com.wenjunhuang.codeepiphany.model.CodeDojo
import com.wenjunhuang.codeepiphany.utils.CompatibleUtils
import com.wenjunhuang.codeepiphany.utils.syntax.*
import okhttp3.*
import org.http4s.client.Client
import org.typelevel.ci.CIString
import org.typelevel.log4cats.LoggerFactory

import java.io.IOException
import java.net.{ HttpCookie, ProxySelector, SocketAddress, URI }
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import java.{ net, util }
import javax.net.ssl.{ SSLContext, TrustManager, X509TrustManager }
import scala.annotation.static
import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*
import scala.util.boundary
import cats.effect.IO

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

  override def getClient: Resource[IO, Client[IO]] = {
    Resource.suspend(IO.delay {
      OkHttpBuilder.fromUnmanaged(defaultHttpClient).resource
    })
  }

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
      .proxySelector(new ProxySelector {
        override def select(uri: URI): util.List[net.Proxy] = {
          val ideaProxySelector = CompatibleUtils.getIdeaProxySelector
          val proxies           = ideaProxySelector.select(uri)
          proxies
        }

        override def connectFailed(uri: URI, sa: SocketAddress, ioe: IOException): Unit = {
          val ideaProxySelector = CompatibleUtils.getIdeaProxySelector
          ideaProxySelector.connectFailed(uri, sa, ioe)
        }
      })
      .proxyAuthenticator(new Authenticator {
        override def authenticate(route: Route, response: Response): Request = {
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
        }
      })
      .addInterceptor((chain: Interceptor.Chain) => {
        val request = chain.request()
        val requestWithUserAgent =
          request
            .newBuilder()
            .header(
              "User-Agent",
              "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0"
            )
            .build()
        val response = chain.proceed(requestWithUserAgent)
        response
      })
      .build()
  }

  private lazy val defaultHttpClient = makeDefaultHttpClient(10.seconds, 10.seconds, 10.seconds)

  @static
  def getHttpClient: OkHttpClient = defaultHttpClient
}
