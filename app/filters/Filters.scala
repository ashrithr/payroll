package filters

import javax.inject.{Inject, Singleton}

import play.api.Environment
import play.api.http.HttpFilters

@Singleton
class Filters @Inject() (
  env: Environment,
  accessLog: AccessLog) extends HttpFilters {

  override val filters = {
    Seq(accessLog)
  }

}
