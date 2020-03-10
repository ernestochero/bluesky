import zio.{ Runtime, ZIO, ZLayer }
import modules.SkyBlueLogger.logger
import zio._
import commons.ZIOHelpers._
import commons.Utils.BufferedImageOps
import commons.ImageUtil.getPathsFromFolder
import modules.configurationModule.{ ConfigurationModule, configuration }
import modules.imageUtilModule.ImageUtilModule
import org.opencv.core.Core
import modules.loggingModule.{ LoggingModule, info }
import modules.qualifyModule.{ QualifyModule, _ }

/*object blueSkyEnv {
  type BlueSkyEnv = LoggingModule with ConfigurationModule with ImageUtilModule with QualifyModule

  object BlueSkyEnv {
    val any: ZLayer[BlueSkyEnv, Nothing, BlueSkyEnv] =
      ZLayer.requires[BlueSkyEnv]

    val live: ZLayer.NoDeps[Nothing, BlueSkyEnv] = {
      ConfigurationModule.live ++  ImageUtilModule.live ++
        (LoggingModule.live >>> QualifyModule.live)
    }
  }
}*/

object Server extends App {
  val appRunTime = Runtime.unsafeFromLayer(liveEnvironments, platform).environment
  val services: ZIO[
    zio.ZEnv with LoggingModule with ConfigurationModule with ImageUtilModule with QualifyModule,
    Throwable,
    Unit
  ] =
    for {
      loadedConfiguration <- modules.configurationModule.configuration
      _                   <- info(s"Starting Program ${loadedConfiguration.appName}")
      t0 = System.nanoTime()
      imageUtilMod    <- modules.imageUtilModule.imageUtil
      bufferedPattern <- imageUtilMod.getBufferedImage(loadedConfiguration.examPath.pattern)
      pathsFromFolder = getPathsFromFolder(loadedConfiguration.examPath.group)
      resultPattern <- process(bufferedPattern.toMat)
      resultGroup <- if (pathsFromFolder.nonEmpty) {
        ZIO.collectAll {
          pathsFromFolder.map(r => {
            for {
              bufferedImg <- imageUtilMod.getBufferedImage(r)
              exam        <- process(bufferedImg.toMat)
            } yield exam
          })
        }
      } else {
        info("incorrect folder path") *> ZIO.fail(
          new Exception("incorrect folder path")
        )
      }
      _ <- showResultFinal(resultPattern, resultGroup)
      t1 = System.nanoTime()
      _ <- info(s"Elapsed time: ${t1 - t0} ns")
      _ <- info(s"Total of Exams Analyzed = ${resultGroup.length}")
    } yield ()

  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] = {
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME)
    services.provide(appRunTime).fold(_ => 1, _ => 0)
  }
}
