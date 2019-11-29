package modules

import org.apache.log4j.Logger

object SkyBlueLogger {
  implicit final val logger: Logger = org.apache.log4j.Logger.getLogger("BlueSkyLogger")
}
