package filters

import javax.inject.{Inject, Singleton}

import akka.stream.Materializer
import play.api.mvc.{Filter, RequestHeader, Result}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AccessLog @Inject()(
  implicit override val mat: Materializer,
  ec: ExecutionContext) extends Filter {

  override def apply(nextFilter: (RequestHeader) => Future[Result])(requestHeader: RequestHeader): Future[Result] = {

    nextFilter(requestHeader).map { result =>
      val msg = s"method=${requestHeader.method} uri=${requestHeader.uri} remote-address=${requestHeader.remoteAddress} " +
        s"domain=${requestHeader.domain} query-string=${requestHeader.rawQueryString} " +
        s"referrer=${requestHeader.headers.get("referrer").getOrElse("N/A")} " +
        s"user-agent=[${requestHeader.headers.get("user-agent").getOrElse("N/A")}]"

      play.Logger.of("accessLog").info(msg)

      result.withHeaders("Request-Time" -> requestHeader.toString())
    }

  }

}
