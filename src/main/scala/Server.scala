import modules.{ ConfigurationModule, ImageUtilModule, LoggingModule, QualifyModule }
import zio.ZIO
import zio.console.Console
import modules.SkyBlueLogger.logger
import zio._
import commons.ZIOHelpers._
import zio.internal.PlatformLive
import commons.Utils.BufferedImageOps
import commons.ImageUtil.getPathsFromFolder
import org.opencv.core.Core
object Server extends App {
  val appRunTime = Runtime(liveEnvironments, PlatformLive.Default)
  val services: ZIO[
    Console with LoggingModule with ConfigurationModule with ImageUtilModule with QualifyModule,
    Throwable,
    Unit
  ] =
    for {
      configuration <- ConfigurationModule.factory.configuration
      _             <- LoggingModule.factory.info(s"Starting Program ${configuration.appName}")
      t0 = System.nanoTime()
      imageUtilModule <- ImageUtilModule.factory.imageUtil
      bufferedPattern <- imageUtilModule.getBufferedImage(configuration.examPath.pattern)
      pathsFromFolder = getPathsFromFolder(configuration.examPath.group)
      qualifyModule <- QualifyModule.factory.qualify
      resultPattern <- qualifyModule.process(bufferedPattern.toMat)
      resultGroup <- if (pathsFromFolder.nonEmpty) {
        ZIO.collectAll {
          pathsFromFolder.map(r => {
            for {
              bufferedImg <- imageUtilModule.getBufferedImage(r)
              exam        <- qualifyModule.process(bufferedImg.toMat)
            } yield exam
          })
        }
      } else {
        LoggingModule.factory.info("incorrect folder path") *> ZIO.fail(
          new Exception("incorrect folder path")
        )
      }
      _ <- qualifyModule.showResultFinal(resultPattern, resultGroup)
      t1 = System.nanoTime()
      _ <- LoggingModule.factory.info(s"Elapsed time: ${t1 - t0} ns")
      _ <- LoggingModule.factory.info(s"Total of Exams Analyzed = ${resultGroup.length}")
    } yield ()

  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] = {
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME)
    services.provide(liveEnvironments).fold(_ => 1, _ => 0)
  }
}
