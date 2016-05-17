package models

import play.api.libs.json.{Reads, Writes}
import reactivemongo.bson.{BSON, BSONHandler, BSONString}
import utils.EnumUtils

object PaymentNet extends Enumeration {

  type Net = Value
  val NET_30 = Value("30")
  val NET_45 = Value("45")
  val NET_60 = Value("60")

  def isNetType(s: String) = values.exists(_.toString == s)

  implicit val enumReads: Reads[Net] = EnumUtils.enumReads(PaymentNet)
  implicit val enumWrites: Writes[Net] = EnumUtils.enumWrites

  implicit object BSONEnumHandler extends BSONHandler[BSONString, Net] {
    def read(doc: BSONString) = PaymentNet.Value(doc.value)
    def write(net: Net) = BSON.write(net.toString)
  }

}
