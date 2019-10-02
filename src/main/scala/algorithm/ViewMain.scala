package algorithm
import java.awt.image.BufferedImage

import org.opencv.core.Core
import ZIOApp._
import scalaz.zio.{ DefaultRuntime, Ref, ZIO, console }
import Utils._
import Calificate._
import scalaz.zio.console.Console

case class ResultExam(valid: Int, wrong: Int)
object execute {
  def main(args: Array[String]): Unit = {
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME)
    if (args.length == 2) {
      val runtime = new DefaultRuntime {}

      val program = for {
        _ <- log("initiating program to calificate exams")
        t0                   = System.nanoTime()
        bufferedImagePattern = Util.loadFileImage(args(0))
        pathsFromFolder      = Util.getPathsFromFolder(args(1))
        resultPattern   <- processBufferedImage(bufferedImagePattern)
        resultListExams <- processImagePaths(pathsFromFolder)
        _               <- showResulFinalZIO(resultPattern, resultListExams)
        t1 = System.nanoTime()
        _                       <- log(s"Elapsed time: ${t1 - t0} ns")
        _                       <- log(s"Total of Exams Analyzed = ${resultListExams.length}")
        logForValidCalification <- getLogs[String]
        _                       <- console.putStrLn(logForValidCalification.mkString("\n"))
      } yield ()

      runtime.unsafeRun(for {
        wref <- Ref.make[Vector[String]](Vector())
        result <- program.provide(new Console.Live with Writer[String] {
          override def writer: Ref[Vector[String]] = wref
        })
      } yield result)

    } else {
      println("incorrect params")
    }
  }

  def compare(pc: Char, ec: Char): Int =
    if (ec == 'y') 0
    else if (pc == ec) 1
    else 2

  def evaluate(result: List[Int]): ResultExam = {
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

  def processResult(result: scala.collection.immutable.List[Answer]): String =
    result.map(_.value).mkString("")

  def showResultInGroups(code: String,
                         aptitude: ResultExam,
                         knowledge: ResultExam): ZIO[Writer[String], Nothing, Unit] =
    log(
      s"exam[${code}] => ${aptitude.valid}/${aptitude.wrong}-${knowledge.valid}/${knowledge.wrong} "
    )

  def showResulFinalZIO(pattern: Exam, exams: List[Exam]): ZIO[Writer[String], Nothing, Unit] =
    for {
      _ <- log("show message result")
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

  def showResultZIO(exam: Exam): ZIO[Writer[String], Nothing, Unit] =
    log(s"code : ${processResult(exam.code)} with alternatives ${processResult(exam.alternatives)}")

  def processBufferedImage(
    bufferedImageStream: Option[BufferedImage]
  ): ZIO[Writer[String], String, Exam] =
    for {
      bufferedImage <- ZIO.fromOption(bufferedImageStream).mapError(_ => "incorrect file path")
      matImage = bufferedImage.toMat
      warpPerspective <- warpPerspectiveOperation(matImage)
      exam            <- qualifyTemplate(warpPerspective)
      _ = showResultZIO(exam)
    } yield exam

  def processImagePaths(
    paths: scala.collection.immutable.List[String]
  ): ZIO[Writer[String], String, List[Exam]] =
    for {
      exams <- if (paths.nonEmpty) {
        ZIO.collectAll(paths.map(r => processBufferedImage(Util.loadFileImage(r))))
      } else log("incorrect folder path") *> ZIO.fail("incorrect folder path")
    } yield exams
}
