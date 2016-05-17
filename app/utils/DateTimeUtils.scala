package utils

import org.joda.time.DateTime

object DateTimeUtils {

  implicit def dateTimeOrdering: Ordering[DateTime] = Ordering.fromLessThan(_ isAfter _)

}
