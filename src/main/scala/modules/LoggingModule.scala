package modules
import org.log4s.Logger
import zio.{ UIO, ZIO }
import LoggingModule._

trait LoggingModule extends Serializable {
  val logger: Service[Any]
}

object LoggingModule {

  trait Service[R] extends Serializable {
    def debug(msg: String)(implicit logger: Logger): ZIO[R, Nothing, Unit]
    def info(msg: String)(implicit logger: Logger): ZIO[R, Nothing, Unit]
    def warn(msg: String)(implicit logger: Logger): ZIO[R, Nothing, Unit]
    def warn(msg: String, exception: Throwable)(implicit logger: Logger): ZIO[R, Nothing, Unit]
    def error(msg: String, exception: Throwable)(implicit logger: Logger): ZIO[R, Nothing, Unit]
  }

  trait Live extends LoggingModule {
    val logger: Service[Any] = new Service[Any] {
      def debug(msg: String)(implicit logger: Logger): UIO[Unit] =
        UIO.effectTotal(logger.debug(msg))

      def info(msg: String)(implicit logger: Logger): UIO[Unit] = UIO.effectTotal(logger.info(msg))

      def warn(msg: String)(implicit logger: Logger): UIO[Unit] = UIO.effectTotal(logger.warn(msg))

      def warn(msg: String, exception: Throwable)(implicit logger: Logger): UIO[Unit] =
        UIO.effectTotal(logger.warn(exception)(msg))

      def error(msg: String, exception: Throwable)(implicit logger: Logger): UIO[Unit] =
        UIO.effectTotal(logger.error(exception)(msg))
    }
  }

  object factory extends Service[LoggingModule] {
    override def debug(msg: String)(implicit logger: Logger): ZIO[LoggingModule, Nothing, Unit] =
      ZIO.accessM[LoggingModule](_.logger.debug(msg))

    override def info(msg: String)(implicit logger: Logger): ZIO[LoggingModule, Nothing, Unit] =
      ZIO.accessM[LoggingModule](_.logger.info(msg))

    override def warn(msg: String)(implicit logger: Logger): ZIO[LoggingModule, Nothing, Unit] =
      ZIO.accessM[LoggingModule](_.logger.warn(msg))

    override def warn(msg: String, exception: Throwable)(
      implicit logger: Logger
    ): ZIO[LoggingModule, Nothing, Unit] =
      ZIO.accessM[LoggingModule](_.logger.warn(msg, exception))

    override def error(msg: String, exception: Throwable)(
      implicit logger: Logger
    ): ZIO[LoggingModule, Nothing, Unit] =
      ZIO.accessM[LoggingModule](_.logger.error(msg, exception))
  }

}
