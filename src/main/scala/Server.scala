import zio.{ Runtime, ZIO }
import modules.SkyBlueLogger.logger
import zio._
import commons.ZIOHelpers._
import commons.Utils.BufferedImageOps
import commons.ImageUtil.getPathsFromFolder
import commons.blueSkyEnv
import org.opencv.core.Core
import modules.loggingModule.info
import modules.qualifyModule._

object Server extends App {
  val appRunTime: zio.ZEnv with blueSkyEnv.BlueSkyEnv =
    Runtime.unsafeFromLayer(liveEnvironments, platform).environment
  val services: ZIO[
    zio.ZEnv with blueSkyEnv.BlueSkyEnv,
    Throwable,
    Unit
  ] =
    for {
      loadedConfiguration <- modules.configurationModule.configuration
      _                   <- info(s"Starting Program ${loadedConfiguration.appName}")
      bufferedPattern <- modules.imageUtilModule.getBufferedImage(
        loadedConfiguration.examPath.pattern
      )
      pathsFromFolder = getPathsFromFolder(loadedConfiguration.examPath.group)
      t0              = System.nanoTime()
      resultPattern <- process(bufferedPattern.toMat)
      resultGroup <- if (pathsFromFolder.nonEmpty) {
        ZIO.foreach(pathsFromFolder)(r => {
          for {
            bufferedImg <- modules.imageUtilModule.getBufferedImage(r)
            exam        <- process(bufferedImg.toMat)
          } yield exam
        })
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

  override def run(args: List[String]) = {
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME)
    services.provide(appRunTime).fold(_ => 1, _ => 0)
  }
}
