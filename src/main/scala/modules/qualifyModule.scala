package modules
import zio.{ Has, ZIO, ZLayer }
import models.{ Answer, Exam }
import org.opencv.core.Mat
import modules.SkyBlueLogger.logger
import modules.loggingModule.LoggingModule

package object qualifyModule {
  type QualifyModule = Has[QualifyModule.Service]
  object QualifyModule {
    import commons.Qualify._
    final case class ResultExam(valid: Int, wrong: Int)
    case class Qualify(logging: LoggingModule.Service) {}
    trait Service {
      def showResultFinal(pattern: Exam, exams: List[Exam]): ZIO[LoggingModule, Nothing, Unit]
      def process(matImg: Mat): ZIO[LoggingModule with QualifyModule, Exception, Exam]
    }

    val live: ZLayer[Has[LoggingModule.Service], Nothing, Has[Service]] =
      ZLayer.fromService { logging: LoggingModule.Service =>
        new Service {
          def showResultFinal(
            pattern: Exam,
            exams: List[Exam]
          ): ZIO[LoggingModule, Nothing, Unit] =
            for {
              _ <- logging.info("message result")
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

          private def compare(pc: Char, ec: Char): Int =
            if (ec == 'y') 0
            else if (pc == ec) 1
            else 2

          private def evaluate(result: List[Int]): ResultExam = {
            val validCount = result.count(_ == 1)
            val wrongCount = result.count(_ == 2)
            ResultExam(validCount, wrongCount)
          }

          private def compareAlternatives(pattern: Exam, exam: Exam): (ResultExam, ResultExam) = {
            val zip       = pattern.alternatives.map(_.value).zip(exam.alternatives.map(_.value))
            val result    = zip.map(z => compare(z._1, z._2))
            val aptitude  = evaluate(result.take(30))
            val knowledge = evaluate(result.takeRight(70))
            (aptitude, knowledge)
          }

          private def processResult(result: List[Answer]): String =
            result.map(_.value).mkString("")

          def process(matImg: Mat): ZIO[LoggingModule with QualifyModule, Exception, Exam] =
            for {
              warpPerspective <- warpPerspectiveOperation(matImg)
              exam            <- qualifyTemplate(warpPerspective)
              _ <- logging.info(
                s"code : ${processResult(exam.code)} with alternatives ${processResult(exam.alternatives)}"
              )
            } yield exam

          private def showResultInGroups(
            code: String,
            aptitude: ResultExam,
            knowledge: ResultExam
          ): ZIO[LoggingModule, Nothing, Unit] =
            logging.info(
              s"exam[$code] => ${aptitude.valid}/${aptitude.wrong}-${knowledge.valid}/${knowledge.wrong} "
            )
        }
      }
  }

  def process(matImg: Mat): ZIO[QualifyModule with LoggingModule, Exception, Exam] =
    ZIO.accessM[QualifyModule with LoggingModule](_.get.process(matImg))

  def showResultFinal(
    pattern: Exam,
    exams: List[Exam]
  ): ZIO[QualifyModule with LoggingModule, Nothing, Unit] =
    ZIO.accessM[QualifyModule with LoggingModule](_.get.showResultFinal(pattern, exams))

}
