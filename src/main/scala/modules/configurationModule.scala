package modules
import zio.{ Has, ZIO, ZLayer }
import pureconfig._
import pureconfig.generic.auto._

package object configurationModule {
  type ConfigurationModule = Has[ConfigurationModule.Service]
  object ConfigurationModule {
    case class ConfigurationError(message: String) extends RuntimeException(message)
    case class ExamPath(pattern: String, group: String)
    case class Configuration(appName: String, examPath: ExamPath)
    trait Service {
      def configuration: ZIO[ConfigurationModule, Throwable, Configuration]
    }
    val live: ZLayer.NoDeps[Nothing, ConfigurationModule] = {
      ZLayer.succeed {
        new Service {
          override def configuration: ZIO[ConfigurationModule, Throwable, Configuration] =
            ZIO
              .fromEither(
                ConfigSource.default.load[Configuration]
              )
              .mapError(e => ConfigurationError(e.toList.mkString(", ")))
        }
      }
    }
  }
  def configuration: ZIO[ConfigurationModule, Throwable, ConfigurationModule.Configuration] =
    ZIO.accessM[ConfigurationModule](_.get.configuration)
}
