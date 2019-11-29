package modules
import zio.ZIO
import QualifyModule._
import models.{ Answer, Exam }
import org.opencv.core.Mat
import modules.Logger.logger
trait QualifyModule {
  val qualifyModule: Service[Any]
}
object QualifyModule {
  import commons.Qualify._
  final case class ResultExam(valid: Int, wrong: Int)
  case class Qualify() {

    private def compare(pc: Char, ec: Char): Int =
      if (ec == 'y') 0
      else if (pc == ec) 1
      else 2

    private def evaluate(result: List[Int]): ResultExam = {
      val validCount = result.count(_ == 1)
      val wrongCount = result.count(_ == 2)
      ResultExam(validCount, wrongCount)
    }

    def compareAlternatives(pattern: Exam, exam: Exam): (ResultExam, ResultExam) = {
      val zip       = pattern.alternatives.map(_.value).zip(exam.alternatives.map(_.value))
      val result    = zip.map(z => compare(z._1, z._2))
      val aptitude  = evaluate(result.take(30))
      val knowledge = evaluate(result.takeRight(70))
      (aptitude, knowledge)
    }

    private def processResult(result: List[Answer]): String =
      result.map(_.value).mkString("")

    def process(matImg: Mat): ZIO[LoggingModule, Exception, Exam] =
      for {
        warpPerspective <- warpPerspectiveOperation(matImg)
        exam            <- qualifyTemplate(warpPerspective)
        _ <- LoggingModule.factory.info(
          s"code : ${processResult(exam.code)} with alternatives ${processResult(exam.alternatives)}"
        )
      } yield exam

    private def showResultInGroups(code: String,
                                   aptitude: ResultExam,
                                   knowledge: ResultExam): ZIO[LoggingModule, Nothing, Unit] =
      LoggingModule.factory.info(
        s"exam[${code}] => ${aptitude.valid}/${aptitude.wrong}-${knowledge.valid}/${knowledge.wrong} "
      )

    def showResultFinal(pattern: Exam, exams: List[Exam]): ZIO[LoggingModule, Nothing, Unit] =
      for {
        _ <- LoggingModule.factory.info("message result")
        results = exams.map { exam =>
          val resultExam = compareAlternatives(pattern, exam)
          showResultInGroups(
            exam.code.map(_.value).mkString(""),
            resultExam._1,
            resultExam._2
          )
        }
        _ <- ZIO.collectAll(results)
      } yield ()
  }
  trait Service[R] {
    def qualify: ZIO[R, Throwable, Qualify]
  }

  trait Live extends QualifyModule {
    override val qualifyModule: Service[Any] = new Service[Any] {
      override def qualify: ZIO[Any, Throwable, Qualify] = ZIO.succeed(Qualify())
    }
  }

  object factory extends Service[QualifyModule] {
    override def qualify: ZIO[QualifyModule, Throwable, Qualify] =
      ZIO.accessM[QualifyModule](
        _.qualifyModule.qualify
      )
  }

}
