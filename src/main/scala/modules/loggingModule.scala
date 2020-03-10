package modules
import org.log4s.Logger
import zio.{ Has, UIO, ZIO, ZLayer }
import modules.configurationModule.ConfigurationModule

package object loggingModule {
  type LoggingModule = Has[LoggingModule.Service]
  object LoggingModule {
    trait Service {
      def debug(msg: String)(implicit logger: Logger): ZIO[LoggingModule, Nothing, Unit]
      def info(msg: String)(implicit logger: Logger): ZIO[LoggingModule, Nothing, Unit]
      def warn(msg: String)(implicit logger: Logger): ZIO[LoggingModule, Nothing, Unit]
      def warn(msg: String, exception: Throwable)(
        implicit logger: Logger
      ): ZIO[LoggingModule, Nothing, Unit]
      def error(msg: String, exception: Throwable)(
        implicit logger: Logger
      ): ZIO[LoggingModule, Nothing, Unit]
    }
    val live: ZLayer.NoDeps[Nothing, ConfigurationModule] =
      ZLayer.succeed {
        new Service {
          override def debug(
            msg: String
          )(implicit logger: Logger): ZIO[LoggingModule, Nothing, Unit] =
            UIO.effectTotal(logger.debug(msg))
          override def info(
            msg: String
          )(implicit logger: Logger): ZIO[LoggingModule, Nothing, Unit] =
            UIO.effectTotal(logger.info(msg))
          override def warn(
            msg: String
          )(implicit logger: Logger): ZIO[LoggingModule, Nothing, Unit] =
            UIO.effectTotal(logger.warn(msg))
          override def warn(msg: String, exception: Throwable)(
            implicit logger: Logger
          ): ZIO[LoggingModule, Nothing, Unit] =
            UIO.effectTotal(logger.warn(exception)(msg))
          override def error(msg: String, exception: Throwable)(
            implicit logger: Logger
          ): ZIO[LoggingModule, Nothing, Unit] =
            UIO.effectTotal(logger.error(exception)(msg))
        }
      }
  }
  def debug(msg: String)(implicit logger: Logger): ZIO[LoggingModule, Nothing, Unit] =
    ZIO.accessM[LoggingModule](_.get.debug(msg))

  def info(msg: String)(implicit logger: Logger): ZIO[LoggingModule, Nothing, Unit] =
    ZIO.accessM[LoggingModule](_.get.info(msg))

  def warn(msg: String)(implicit logger: Logger): ZIO[LoggingModule, Nothing, Unit] =
    ZIO.accessM[LoggingModule](_.get.warn(msg))

  def warn(msg: String, exception: Throwable)(
    implicit logger: Logger
  ): ZIO[LoggingModule, Nothing, Unit] =
    ZIO.accessM[LoggingModule](_.get.warn(msg, exception))

  def error(msg: String, exception: Throwable)(
    implicit logger: Logger
  ): ZIO[LoggingModule, Nothing, Unit] =
    ZIO.accessM[LoggingModule](_.get.error(msg, exception))

}
