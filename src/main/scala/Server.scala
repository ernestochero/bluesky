import modules.{ ConfigurationModule, LoggingModule }
import zio.ZIO
import zio.console.{ Console, putStrLn }
import modules.Logger.logger
import zio._
import commons.ZIOHelpers._
import zio.internal.PlatformLive
object Server extends App {
  val appRunTime = Runtime(liveEnvironments, PlatformLive.Default)
  val services: ZIO[Console with LoggingModule with ConfigurationModule, Throwable, Unit] =
    for {
      configuration <- ConfigurationModule.factory.configuration
      _             <- LoggingModule.factory.info(s"Starting Program ${configuration.appName}")
    } yield ()

  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] =
    services.provide(liveEnvironments).fold(_ => 1, _ => 0)
}
