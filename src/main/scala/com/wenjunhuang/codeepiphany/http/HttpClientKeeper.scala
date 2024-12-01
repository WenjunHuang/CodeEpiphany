package com.wenjunhuang.codeepiphany.http

import cats.effect.{Async, Resource}
import cats.effect.kernel.Ref.Make
import cats.effect.kernel.Sync
import com.intellij.util.net.*
import okhttp3.{Authenticator, ConnectionPool, Credentials, Dispatcher, OkHttpClient, Request, Response, Route}
import org.http4s.client.Client

import java.security.cert.X509Certificate
import javax.net.ssl.{SSLContext, TrustManager, X509TrustManager}
import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*
import scala.util.boundary
import com.wenjunhuang.codeepiphany.runtime.intellijComputeContext

trait HttpClientKeeper[F[_]] {
  def getClient: Resource[F, Client[F]]
}

object HttpClientKeeper {
  def apply[F[_]: Make: Async](): HttpClientKeeper[F] =
    new HttpClientKeeper[F] {
      override def getClient: Resource[F, Client[F]] =
        Resource.suspend(Sync[F].delay {
          val proxySettings = ProxySettings.getInstance()
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
                  override def authenticate(route: Route, response: Response): Request = {
                    println("ok")
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
                }
                builder.proxyAuthenticator(authenticator)
              OkHttpBuilder[F](builder.build()).resource
          }
        })
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
