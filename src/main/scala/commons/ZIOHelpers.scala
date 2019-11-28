package commons

import modules.{ ConfigurationModule, LoggingModule }
import zio.{ Task, ZIO }
import zio.clock.Clock
import zio.random.Random
import zio.system.System
import zio.console.Console
import zio.blocking.Blocking

import scala.concurrent.Future
object ZIOHelpers {
  type AppEnvironment = zio.ZEnv with ConfigurationModule with LoggingModule
  val liveEnvironments = new System.Live with Clock.Live with Console.Live with Blocking.Live
  with Random.Live with ConfigurationModule.Live with LoggingModule.Live

  def fromFuture[A](f: Future[A]): Task[A] =
    ZIO.fromFuture(implicit ec => f.map(a => a))

  def eradicateNull[E, A](possiblyNullValue: A, errOnNull: E): ZIO[Any, E, A] =
    Option(possiblyNullValue).map(ZIO.succeed).getOrElse(ZIO.fail(errOnNull))
}
