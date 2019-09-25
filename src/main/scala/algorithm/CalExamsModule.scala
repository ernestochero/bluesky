package algorithm

import java.awt.image.BufferedImage
import java.io.InputStream

import org.opencv.core.{ Mat, MatOfPoint, MatOfPoint2f }
import scalaz._
import Scalaz._

final case class Answer(index: Int, value: Char, category: String)
final case class Exam(code: List[Answer], alternatives: List[Answer])
final case class Size(width: Int, height: Int)
final case class Mark(id: Int, mat: Mat)
final case class Point(x: Int, y: Int)
final case class WorldView(
  exams: List[Exam],
  pattern: Exam,
  managed: List[Exam],
  pending: List[Exam]
)
case class ResultExam(valid: Int, wrong: Int)
case class SummaryResultExam(aptitude: ResultExam, knowledge: ResultExam)
trait Image[F[_]] {
  def bufferedImageToMat(bufferedImage: BufferedImage): F[Mat]
  def matToBufferedImage(mat: Mat): F[BufferedImage]
  def grayScaleOperation(mat: Mat): F[Mat]
  def thresholdOperation(mat: Mat): F[Mat]
  def dilatationOperation(mat: Mat): F[Mat]
  def erodeOperation(mat: Mat): F[Mat]
  def gaussianBlurOperation(mat: Mat): F[Mat]
  def cannyOperation(mat: Mat): F[Mat]
  def openOperation(mat: Mat): F[Mat]
  def closeOperation(mat: Mat): F[Mat]
}

trait Calificate[F[_]] {
  def distance(p1: Point, p2: Point): F[Double]
  def sortPointsWithReference(point1: Point, point2: Point, pointReference: Point): F[Boolean]
  def sortPoints(points: MatOfPoint2f, refer: Point): F[MatOfPoint2f]
  def getSizeOfQuad(points: MatOfPoint2f): F[Size]
  def sortMatOfPoint(mop1: MatOfPoint, mop2: MatOfPoint): F[Boolean]
  def findSquare(list: List[MatOfPoint]): F[Option[MatOfPoint2f]]
  def warpPerspectiveOperationOpt(mat: Mat): F[Option[Mat]]
  def sortedGroups(group: List[MatOfPoint], t: Int): F[List[MatOfPoint]]
  def filterContour(contour: MatOfPoint): F[Boolean]
  def evaluateBubble(bubble: MatOfPoint, closed: Mat): F[Int]
  def findContoursOperationOpt(matThreshold: Mat, typeSection: String): F[Option[List[List[Int]]]]
  def qualifyTemplateOpt(img: Mat): F[Option[Exam]]
  def getCodeOfMatrix(matrix: List[List[Int]]): F[List[Answer]]
  def getAnswersOfMatrix(matrix: List[List[Int]]): F[List[Answer]]
  def getCornersTuple(ids: Mat, corners: List[Mat]): F[List[Mark]]
  def getCenterSquare(mark: Mark): F[Point]
  def getSubMat(pointA: Point, pointB: Point, img: Mat): F[Mat]
}

trait ViewMain[F[_]] {
  def loadImage(inputStream: InputStream): F[BufferedImage]
  def createInputStream(path: String): F[Option[InputStream]]
  def getPattern(path: String): F[Option[BufferedImage]]
  def getPathsFromFolder(path: String): F[List[String]]
  def processBufferedImage(bufferedImageStream: Option[BufferedImage]): F[Option[Exam]]
  def processImagePaths(paths: scala.collection.immutable.List[String]): List[Option[Exam]]
  def showResult(result: Option[Exam]): F[Unit]
  def showResultFinal(examPatternOpt: Option[Exam], exams: List[Option[Exam]]): F[Unit]
  def showResultInGroups(code: String, aptitude: ResultExam, knowledge: ResultExam): F[Unit]
  def compareAlternatives(pattern: Exam, exam: Exam): F[SummaryResultExam]
  def evaluate(result: List[Int]): F[ResultExam]
  def compare(pc: Char, ec: Char): F[Int]
}

trait CalExams[F[_]] {
  def initial: F[WorldView]
  def calificate(old: WorldView): F[WorldView]
  def act(world: WorldView): F[WorldView]
  def stop: F[WorldView]
}

final case class CalExamsModule[F[_]: Monad](I: Image[F], C: Calificate[F]) extends CalExams[F] {
  override def initial: F[WorldView] = ???

  override def calificate(old: WorldView): F[WorldView] = ???

  override def act(world: WorldView): F[WorldView] = ???

  override def stop: F[WorldView] = ???
}
