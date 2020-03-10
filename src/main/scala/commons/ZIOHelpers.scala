package commons

import modules.configurationModule.ConfigurationModule
import modules.imageUtilModule.ImageUtilModule
import modules.loggingModule.LoggingModule
import modules.qualifyModule.QualifyModule
import zio.{ Task, ZIO }
import scala.concurrent.Future
object ZIOHelpers {
  type AppEnvironment = zio.ZEnv
    with ConfigurationModule
    with ImageUtilModule
    with LoggingModule
    with QualifyModule

  val liveEnvironments =
  zio.ZEnv.live ++ ConfigurationModule.live ++ ImageUtilModule.live ++ LoggingModule.live ++ QualifyModule.live

  def fromFuture[A](f: Future[A]): Task[A] =
    ZIO.fromFuture(implicit ec => f.map(a => a))

  def eradicateNull[E, A](possiblyNullValue: A, errOnNull: E): ZIO[Any, E, A] =
    Option(possiblyNullValue).map(ZIO.succeed).getOrElse(ZIO.fail(errOnNull))
}
