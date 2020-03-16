package commons

import commons.blueSkyEnv.BlueSkyEnv
import modules.configurationModule.ConfigurationModule
import modules.imageUtilModule.ImageUtilModule
import modules.loggingModule.LoggingModule
import modules.qualifyModule.QualifyModule
import zio.{ Has, Task, ZIO, ZLayer }

import scala.concurrent.Future

object blueSkyEnv {
  type BlueSkyEnv = LoggingModule with ConfigurationModule with ImageUtilModule with QualifyModule
  object BlueSkyEnv {
    val any: ZLayer[BlueSkyEnv, Nothing, BlueSkyEnv] =
      ZLayer.requires[BlueSkyEnv]
    val x: ZLayer[Any, Nothing, Has[QualifyModule.Service]] = (LoggingModule.live >>> QualifyModule.live)

    val live = {
      LoggingModule.live ++
      ConfigurationModule.live ++ ImageUtilModule.live ++
      (LoggingModule.live >>> QualifyModule.live)
    }
  }
}

object ZIOHelpers {
  type AppEnvironment = zio.ZEnv with blueSkyEnv.BlueSkyEnv
  val liveEnvironments: ZLayer[Any, Nothing, zio.ZEnv with BlueSkyEnv] =
  zio.ZEnv.live ++ blueSkyEnv.BlueSkyEnv.live

  def fromFuture[A](f: Future[A]): Task[A] =
    ZIO.fromFuture(implicit ec => f.map(a => a))
}
