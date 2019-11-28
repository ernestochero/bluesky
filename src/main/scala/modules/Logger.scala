package modules

import org.apache.log4j.Logger

object Logger {
  implicit final val logger: Logger = org.apache.log4j.Logger.getLogger("volskayaLogger")
}
