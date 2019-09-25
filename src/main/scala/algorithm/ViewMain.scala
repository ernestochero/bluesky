package algorithm
import java.awt.image.BufferedImage
import org.opencv.core.Core
import Algorithm._

object execute {
  case class ResultExam(valid: Int, wrong: Int)

  def main(args: Array[String]): Unit = {
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME)
    if (args.length == 2) {
      val t0                   = System.nanoTime()
      val bufferedImagePattern = Util.loadFileImage(args(0))
      val pathsFromFolder      = Util.getPathsFromFolder(args(1))

      val resultPattern   = processBufferedImage(bufferedImagePattern)
      val resultListExams = processImagePaths(pathsFromFolder)

      showResultFinal(resultPattern, resultListExams)

      val t1 = System.nanoTime()
      println("Elapsed time: " + (t1 - t0) + "ns")
      println(s"Total of Exams Analyzed  = ${resultListExams.length}")

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

  def showResultInGroups(code: String, aptitude: ResultExam, knowledge: ResultExam): Unit =
    println(
      s"${aptitude.valid}/${aptitude.wrong}-${knowledge.valid}/${knowledge.wrong} "
    )
  /*println(
      s"code : $code result: ${aptitude.valid}/${aptitude.wrong}-${knowledge.valid}/${knowledge.wrong} "
    )*/

  def showResultFinal(examPatternOpt: Option[Exam], exams: List[Option[Exam]]): Unit =
    examPatternOpt match {
      case Some(pattern) =>
        exams.foreach(examOpt => {
          examOpt match {
            case Some(exam) =>
              val alternativesCompared = compareAlternatives(pattern, exam)
              showResultInGroups(processResult(exam.code),
                                 alternativesCompared._1,
                                 alternativesCompared._2)
            case None =>
              println("Impossible to Process Exam")
          }
        })
      case None =>
        println("Impossible to Process Exam Pattern")

    }

  def showResult(result: Option[Exam]): Unit =
    result.foreach { exam =>
      println(
        s"code : ${processResult(exam.code)} with alternatives ${processResult(exam.alternatives)}"
      )
    }

  def processBufferedImage(bufferedImageStream: Option[BufferedImage]): Option[Exam] =
    bufferedImageStream match {
      case Some(bufferedImage) =>
        val matImage        = bufferedImageToMat(bufferedImage)
        val warpPerspective = warpPerspectiveOperationOpt(matImage)
        val result          = warpPerspective.flatMap(qualifyTemplateOpt)
        showResult(result)
        result
      case _ =>
        println("incorrect file path")
        None
    }

  def processImagePaths(paths: scala.collection.immutable.List[String]) =
    if (paths.nonEmpty) {
      paths.map { path =>
        processBufferedImage(Util.loadFileImage(path))
      }
    } else {
      println("incorrect folder path")
      scala.collection.immutable.List.empty[Option[Exam]]
    }
}
