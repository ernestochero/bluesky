package modules
import org.log4s.getLogger

object SkyBlueLogger {
  implicit final val logger: org.log4s.Logger = getLogger("BlueSkyLogger")
}
