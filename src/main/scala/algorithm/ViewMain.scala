package algorithm
import java.awt.image.BufferedImage
import org.opencv.core.Core
import Algorithm._
object execute {
  def main(args: Array[String]): Unit = {
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME)
    if(args.length == 2) {
      val t0 = System.nanoTime()
      val bufferedImagePattern = Util.loadFileImage(args(0))
      val pathsFromFolder = Util.getPathsFromFolder(args(1))

      processBufferedImage(bufferedImagePattern)
      val resultListExams = processImagePaths(pathsFromFolder)
      val t1 = System.nanoTime()
      println("Elapsed time: " + (t1 - t0) + "ns")
      println(s"Total of Exams Analyzed  = ${resultListExams.length}")

    } else {
      println("incorrect params")
    }

  }

  def processResult(result: scala.collection.immutable.List[Answer]): String = {
    result.map(_.value).mkString("")
  }

  def showResult(result:Option[Exam]): Unit = {
    result.foreach { exam =>
      println(s"code : ${processResult(exam.code)} with alternatives ${processResult(exam.alternatives)}")
    }
  }

  def processBufferedImage(bufferedImageStream: Option[BufferedImage]): Option[Exam] = bufferedImageStream match {
    case Some(bufferedImage) =>
      val matImage = bufferedImageToMat(bufferedImage)
      val warpPerspective = warpPerspectiveOperationOpt(matImage)
      val result = warpPerspective.flatMap(qualifyTemplateOpt)
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
