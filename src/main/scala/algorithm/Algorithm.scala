package algorithm

import java.awt.image.BufferedImage

import algorithm.Algorithm.{ ANSWER_42, ANSWER_45, CODE_12, CODE_13 }

import scala.util.control.Breaks.{ break, breakable }
import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer
import org.opencv.core._
import org.opencv.imgproc.Imgproc
import org.opencv.aruco.Aruco
import org.opencv.core
import scalaz.zio.{ Ref, ZIO }

import scala.util.Try
import scala.util.Success

case class Answer(index: Int, value: Char, category: String)
case class Exam(code: List[Answer], alternatives: List[Answer])
case class SizeM(width: Double, height: Double)
case class Mark(id: Int, mat: Mat)

trait Writer[W] {
  def writer: Ref[Vector[W]]
}
object ZIOApp {
  type MyZIO[W, E, A] = ZIO[Writer[W], E, A]
  type Error          = String
  type Log            = Vector[String]

  // Writer helpers:
  def log[W](w: W): ZIO[Writer[W], Nothing, Unit] =
    ZIO.accessM[Writer[W]](_.writer.update(vector => vector :+ w)).unit
  def getLogs[W]: ZIO[Writer[W], Nothing, Vector[W]] =
    ZIO.accessM[Writer[W]](_.writer.get)
  def clearLogs[W]: ZIO[Writer[W], Nothing, Unit] =
    ZIO.accessM[Writer[W]](_.writer.set(Vector()))
}

object Image {
  def bufferedImageToMat(bufferedImage: BufferedImage): Mat = {
    val width  = bufferedImage.getWidth()
    val height = bufferedImage.getHeight()
    val mat = bufferedImage.getType match {
      case BufferedImage.TYPE_INT_RGB =>
        val out = new Mat(bufferedImage.getHeight, bufferedImage.getWidth, CvType.CV_8UC3)
        val data =
          new Array[Byte](width * height * out.elemSize.asInstanceOf[Int])
        val dataBuff = bufferedImage.getRGB(0, 0, width, height, null, 0, width)
        for ((x, i) <- dataBuff.view.zipWithIndex) {
          data(i * 3) = ((x >> 16) & 0xFF).asInstanceOf[Byte]
          data(i * 3 + 1) = ((x >> 8) & 0xFF).asInstanceOf[Byte]
          data(i * 3 + 2) = ((x >> 0) & 0xFF).asInstanceOf[Byte]
        }
        out.put(0, 0, data)
        out
      case _ =>
        val out = new Mat(bufferedImage.getHeight, bufferedImage.getWidth, CvType.CV_8UC1)
        val data =
          new Array[Byte](width * height * out.elemSize.asInstanceOf[Int])
        val dataBuff = bufferedImage.getRGB(0, 0, width, height, null, 0, width)
        for ((x, i) <- dataBuff.view.zipWithIndex) {
          data(i) = ((0.21 * ((x >> 16) & 0xFF)) +
          (0.71 * ((x >> 8) & 0xFF)) +
          (0.07 * ((x >> 0) & 0xFF)))
            .asInstanceOf[Byte]
        }
        out.put(0, 0, data)
        out
    }
    mat
  }

  def matToBufferedImage(mat: Mat): BufferedImage = {
    val width  = mat.cols()
    val height = mat.rows()
    val data =
      new Array[Byte](mat.rows * mat.cols * mat.elemSize.asInstanceOf[Int])
    mat.get(0, 0, data)
    val _type = mat.channels match {
      case 1 => BufferedImage.TYPE_BYTE_GRAY
      case _ => BufferedImage.TYPE_3BYTE_BGR
    }
    val bufferedImage = new BufferedImage(width, height, _type)
    bufferedImage.getRaster.setDataElements(0, 0, width, height, data)
    bufferedImage
  }

  def grayScaleOperation(mat: Mat): Mat = {
    val out = new Mat(mat.rows(), mat.cols(), CvType.CV_8UC1)
    Imgproc.cvtColor(mat, out, Imgproc.COLOR_RGB2GRAY)
    out
  }
  // 165
  def thresholdOperation(mat: Mat): Mat = {
    val out = new Mat(mat.rows(), mat.cols(), CvType.CV_8UC1)
    Imgproc.threshold(mat, out, 170, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C)
    out
  }

  def dilatationOperation(mat: Mat): Mat = {
    val out = new Mat(mat.rows(), mat.cols(), CvType.CV_8UC1)
    Imgproc.dilate(
      mat,
      out,
      Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2, 2))
    )
    out
  }

  def erodeOperation(mat: Mat): Mat = {
    val out = new Mat(mat.rows(), mat.cols(), CvType.CV_8UC1)
    Imgproc.erode(
      mat,
      out,
      Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(2, 2))
    )
    out
  }

  def gaussianBlurOperation(mat: Mat): Mat = {
    val out = new Mat(mat.rows(), mat.cols(), CvType.CV_8UC1)
    Imgproc.GaussianBlur(mat, out, new Size(5, 5), 0)
    out
  }

  def cannyOperation(mat: Mat): Mat = {
    val out = new Mat(mat.rows(), mat.cols(), CvType.CV_8UC1)
    Imgproc.Canny(mat, out, 75, 200)
    out
  }

  def openOperation(mat: Mat): Mat = {
    val out = new Mat(mat.rows(), mat.cols(), CvType.CV_8UC1)
    Imgproc.morphologyEx(
      mat,
      out,
      Imgproc.MORPH_OPEN,
      Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(2, 2))
    )
    out
  }

  def closeOperation(mat: Mat): Mat = {
    val out = new Mat(mat.rows(), mat.cols(), CvType.CV_8UC1)
    Imgproc.morphologyEx(
      mat,
      out,
      Imgproc.MORPH_CLOSE,
      Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(4, 4))
    )
    out
  }
}

trait ArrayLike2f[T] {
  def sort(arr: T, ref: Point): T
  def getSizeOfSquad(arr: T): Size
}

object ArrayLike {
  def distance(p1: Point, p2: Point): Double = {
    val d1 = math.pow(p1.x - p2.x, 2)
    val d2 = math.pow(p1.y - p2.y, 2)
    math.sqrt(d1 + d2)
  }

  // _ < _
  def sortPointsWithReference(point1: Point, point2: Point, pointReference: Point): Boolean = {
    val distance1 = distance(point1, pointReference)
    val distance2 = distance(point2, pointReference)
    distance1 < distance2
  }

  implicit object ArrayLikeMatOfPoint2f extends ArrayLike2f[MatOfPoint2f] {
    def sort(points: MatOfPoint2f, ref: Point): MatOfPoint2f = {
      val pointsSorted    = points.toArray.sortWith((p1, p2) => sortPointsWithReference(p1, p2, ref))
      val subPointsSorted = Array(pointsSorted(1), pointsSorted(2)).sortWith(_.x < _.x)
      val x1y1            = pointsSorted(0)
      val x3y3            = pointsSorted(3)
      val x4y4            = subPointsSorted(0)
      val x2y2            = subPointsSorted(1)
      new MatOfPoint2f(x1y1, x2y2, x3y3, x4y4)
    }

    def getSizeOfSquad(points: MatOfPoint2f): Size = {
      val arr    = points.toArray
      val width  = ArrayLike.distance(arr(0), arr(1))
      val height = ArrayLike.distance(arr(0), arr(3))
      new Size(width, height)
    }
  }
}

object Helpers {
  import Constants._

  def getCornersTuple(ids: Mat, corners: List[Mat]): List[Mark] =
    corners
      .foldLeft((List[Mark](), 0)) { (result, value) =>
        val id    = ids.get(result._2, 0)(0)
        val index = result._2 + 1
        (result._1 :+ Mark(id.toInt, value), index)
      }
      ._1

  def getCenterSquare(_id: Int, corner: Mat): Point = {
    // size of mat = 4 x 1 => { (x1,y1) , (x2,y1) , (x2,y2) , (x1,y2) }
    val x1_y1 = new Point(corner.get(0, 0)(0), corner.get(0, 0)(1))
    val x2_y2 = new Point(corner.get(0, 2)(0), corner.get(0, 2)(1))
    _id match {
      case CODE_12   => x2_y2
      case CODE_13   => x1_y1
      case ANSWER_42 => x2_y2
      case _         => x1_y1
    }
  }

  def getSubMat(c1: Point, c2: Point, img: Mat): Mat =
    img.submat(c1.y.toInt, c2.y.toInt, c1.x.toInt, c2.x.toInt)

  def filterContour(contour: MatOfPoint): Boolean = {
    val rect = Imgproc.boundingRect(contour)
    val ar   = rect.width / rect.height.toFloat
    rect.width >= 20 && rect.height >= 20 && ar >= 0.6 && ar <= 1.4
  }

  def sortedGroups(group: List[MatOfPoint], t: Int): List[MatOfPoint] =
    group.sortWith((o1, o2) => {
      val rect1 = Imgproc.boundingRect(o1)
      val rect2 = Imgproc.boundingRect(o2)
      t match {
        case HORIZONTALLY => rect1.tl().x < rect2.tl().x
        case _            => rect1.tl().y < rect2.tl().y
      }
    })

  def evaluateBubble(bubble: MatOfPoint, closed: Mat): Int = {
    val mask = Mat.zeros(closed.size(), CvType.CV_8UC1)
    Imgproc.drawContours(
      mask,
      List(bubble).asJava,
      -1,
      new Scalar(255, 255, 255),
      -1
    )
    val totalBlankMax = (Core.countNonZero(mask) * 0.75).toInt
    Core.bitwise_and(closed, closed, mask, mask)
    val total = Core.countNonZero(mask)
    if (total >= totalBlankMax) 1
    else 0
  }

  def getCodeOfMatrix(matrix: List[List[Int]]): List[Answer] = {
    def f(group: List[Int]): Char = {
      val sumMarks = group.sum
      if (sumMarks == 0) 'y'
      else if (sumMarks == 1) s"${group.indexOf(1)}".charAt(0)
      else 'x'
    }
    val result = matrix.foldLeft((List.empty[Answer], 0)) { (acc, value) =>
      (acc._1 :+ Answer(acc._2, f(value), CODE), acc._2 + 1)
    }
    result._1.sortBy(_.index)
  }

  def getAnswersOfMatrix(matrix: List[List[Int]]): List[Answer] = {

    def f(group: List[Int], env: Map[Int, Char]): Char = {
      val sumMarks = group.sum
      if (sumMarks == 0) 'y'
      else if (sumMarks == 1) env(group.indexOf(1))
      else 'x'
    }

    matrix.par
      .foldLeft((List.empty[Answer], 1)) { (acc, value) =>
        val env   = Map(0 -> 'a', 1 -> 'b', 2 -> 'c', 3 -> 'd', 4 -> 'e')
        val chars = value.grouped(5).map(f(_, env)).toArray
        val answers = chars
          .foldLeft((List.empty[Answer], 0)) { (a, b) =>
            (a._1 :+ Answer(acc._2 + a._2, b, ANSWER), a._2 + 25)
          }
          ._1
        (acc._1 ++ answers, acc._2 + 1)
      }
      ._1
      .sortBy(_.index)
  }

}

object Constants {
  val CODE                    = "code"
  val ANSWER                  = "answer"
  val numberContoursOfCodes   = 70
  val numberContoursOfAnswers = 500

  val HORIZONTALLY = 1
  val VERTICALLY   = 2
}

object Calificate {
  import ZIOApp._
  import Helpers._
  import Image._
  import Constants._
  def sortPoints(points: MatOfPoint2f, refer: Point)(
    implicit ev: ArrayLike2f[MatOfPoint2f]
  ): MyZIO[Log, Error, MatOfPoint2f] = ZIO.succeed(ev.sort(points, refer))

  def calificateExam(corners: java.util.List[Mat],
                     ids: Mat,
                     img: Mat): ZIO[Writer[String], String, Exam] =
    if (!corners.isEmpty && corners.size() == 4) {
      val pointsWithCenter = getCornersTuple(ids, corners.asScala.toList)
        .flatMap(markWithCode => {
          Map(markWithCode.id -> getCenterSquare(markWithCode.id, markWithCode.mat))
        })
        .toMap
      for {
        _ <- log("...")
        pointsWithCenter = getCornersTuple(ids, corners.asScala.toList)
          .flatMap(markWithCode => {
            Map(markWithCode.id -> getCenterSquare(markWithCode.id, markWithCode.mat))
          })
          .toMap
        codeMat         = getSubMat(pointsWithCenter(CODE_12), pointsWithCenter(CODE_13), img)
        alternativesMap = getSubMat(pointsWithCenter(ANSWER_42), pointsWithCenter(ANSWER_45), img)
        codeContours         <- findContoursOperation(thresholdOperation(codeMat), CODE)
        alternativesContours <- findContoursOperation(thresholdOperation(alternativesMap), ANSWER)
        codeResult         = getCodeOfMatrix(codeContours)
        alternativesResult = getAnswersOfMatrix(alternativesContours)
      } yield Exam(codeResult, alternativesResult)

    } else {
      log(s"impossible to process this list of corners") *> ZIO.fail("Error in corners")
    }

  def findContoursOperationProcess(groupLength: Int,
                                   typeSection: String,
                                   contoursList: List[MatOfPoint],
                                   img: Mat): ZIO[Writer[String], String, List[List[Int]]] =
    typeSection match {
      case ANSWER =>
        ZIO.succeed {
          (sortedGroups(contoursList, VERTICALLY)
            .grouped(groupLength)
            .map(
              sortedGroups(_, HORIZONTALLY)
                .map(bubble => evaluateBubble(bubble, img))
            ))
            .toList
        }
      case CODE =>
        ZIO.succeed {
          (sortedGroups(contoursList, HORIZONTALLY)
            .grouped(groupLength)
            .map(
              sortedGroups(_, VERTICALLY)
                .map(bubble => evaluateBubble(bubble, img))
            ))
            .toList
        }
      case _ => log(s"not match typeSection") *> ZIO.fail("match error")
    }

  def findContoursOperation(img: Mat,
                            typeSection: String): ZIO[Writer[String], String, List[List[Int]]] =
    for {
      _ <- log("Starting contours operation")
      out      = new Mat(img.rows(), img.cols(), CvType.CV_8UC3)
      contours = ListBuffer(List[MatOfPoint](): _*).asJava
      closed   = closeOperation(img)
      _ = Imgproc.findContours(closed,
                               contours,
                               out,
                               Imgproc.RETR_EXTERNAL,
                               Imgproc.CHAIN_APPROX_SIMPLE)
      contoursList = contours.asScala.filter(filterContour).toList
      (quantity, groupDivide) = if (typeSection == ANSWER) (numberContoursOfAnswers, 20)
      else (numberContoursOfCodes, 10)
      result <- if (contoursList.length == quantity)
        findContoursOperationProcess(groupDivide, typeSection, contoursList, closed)
      else
        log(s"[$typeSection] number of contours below limit ${contoursList.length} < $quantity") *> ZIO
          .fail("contours below limit")
    } yield result

  def qualifyTemplate(img: Mat): ZIO[Writer[String], String, Exam] =
    for {
      _ <- log(s"Starting ...")
      dictionary = Aruco.getPredefinedDictionary(Aruco.DICT_4X4_50)
      corners    = ListBuffer(List[Mat](): _*).asJava
      ids        = new Mat()
      _          = Aruco.detectMarkers(img, dictionary, corners, ids)
      exam <- calificateExam(corners, ids, img)
    } yield exam
}

object Algorithm {

  val CODE_12   = 12
  val CODE_13   = 13
  val ANSWER_42 = 42
  val ANSWER_45 = 45

  val CODE   = "code"
  val ANSWER = "answer"

  val numberContoursOfCodes   = 70
  val numberContoursOfAnswers = 500

  val HORIZONTALLY = 1
  val VERTICALLY   = 2

  def bufferedImageToMat(bufferedImage: BufferedImage): Mat = {
    val width  = bufferedImage.getWidth()
    val height = bufferedImage.getHeight()
    val mat = bufferedImage.getType match {
      case BufferedImage.TYPE_INT_RGB =>
        val out = new Mat(bufferedImage.getHeight, bufferedImage.getWidth, CvType.CV_8UC3)
        val data =
          new Array[Byte](width * height * out.elemSize.asInstanceOf[Int])
        val dataBuff = bufferedImage.getRGB(0, 0, width, height, null, 0, width)
        for ((x, i) <- dataBuff.view.zipWithIndex) {
          data(i * 3) = ((x >> 16) & 0xFF).asInstanceOf[Byte]
          data(i * 3 + 1) = ((x >> 8) & 0xFF).asInstanceOf[Byte]
          data(i * 3 + 2) = ((x >> 0) & 0xFF).asInstanceOf[Byte]
        }
        out.put(0, 0, data)
        out
      case _ =>
        val out = new Mat(bufferedImage.getHeight, bufferedImage.getWidth, CvType.CV_8UC1)
        val data =
          new Array[Byte](width * height * out.elemSize.asInstanceOf[Int])
        val dataBuff = bufferedImage.getRGB(0, 0, width, height, null, 0, width)
        for ((x, i) <- dataBuff.view.zipWithIndex) {
          data(i) = ((0.21 * ((x >> 16) & 0xFF)) +
          (0.71 * ((x >> 8) & 0xFF)) +
          (0.07 * ((x >> 0) & 0xFF)))
            .asInstanceOf[Byte]
        }
        out.put(0, 0, data)
        out
    }
    mat
  }

  def matToBufferedImage(mat: Mat): BufferedImage = {
    val width  = mat.cols()
    val height = mat.rows()
    val data =
      new Array[Byte](mat.rows * mat.cols * mat.elemSize.asInstanceOf[Int])
    mat.get(0, 0, data)
    val _type = mat.channels match {
      case 1 => BufferedImage.TYPE_BYTE_GRAY
      case _ => BufferedImage.TYPE_3BYTE_BGR
    }
    val bufferedImage = new BufferedImage(width, height, _type)
    bufferedImage.getRaster.setDataElements(0, 0, width, height, data)
    bufferedImage
  }

  def grayScaleOperation(mat: Mat): Mat = {
    val out = new Mat(mat.rows(), mat.cols(), CvType.CV_8UC1)
    Imgproc.cvtColor(mat, out, Imgproc.COLOR_RGB2GRAY)
    out
  }
// 165
  def thresholdOperation(mat: Mat): Mat = {
    val out = new Mat(mat.rows(), mat.cols(), CvType.CV_8UC1)
    Imgproc.threshold(mat, out, 170, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C)
    out
  }

  def dilatationOperation(mat: Mat): Mat = {
    val out = new Mat(mat.rows(), mat.cols(), CvType.CV_8UC1)
    Imgproc.dilate(
      mat,
      out,
      Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2, 2))
    )
    out
  }

  def erodeOperation(mat: Mat): Mat = {
    val out = new Mat(mat.rows(), mat.cols(), CvType.CV_8UC1)
    Imgproc.erode(
      mat,
      out,
      Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(2, 2))
    )
    out
  }

  def gaussianBlurOperation(mat: Mat): Mat = {
    val out = new Mat(mat.rows(), mat.cols(), CvType.CV_8UC1)
    Imgproc.GaussianBlur(mat, out, new Size(5, 5), 0)
    out
  }

  def cannyOperation(mat: Mat): Mat = {
    val out = new Mat(mat.rows(), mat.cols(), CvType.CV_8UC1)
    Imgproc.Canny(mat, out, 75, 200)
    out
  }

  def openOperation(mat: Mat): Mat = {
    val out = new Mat(mat.rows(), mat.cols(), CvType.CV_8UC1)
    Imgproc.morphologyEx(
      mat,
      out,
      Imgproc.MORPH_OPEN,
      Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(2, 2))
    )
    out
  }

  def closeOperation(mat: Mat): Mat = {
    val out = new Mat(mat.rows(), mat.cols(), CvType.CV_8UC1)
    Imgproc.morphologyEx(
      mat,
      out,
      Imgproc.MORPH_CLOSE,
      Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(4, 4))
    )
    out
  }

  def distance(p1: Point, p2: Point): Double = {
    val d1 = math.pow(p1.x - p2.x, 2)
    val d2 = math.pow(p1.y - p2.y, 2)
    math.sqrt(d1 + d2)
  }

  // _ < _
  def sortPointsWithReference(point1: Point, point2: Point, pointReference: Point): Boolean = {
    val distance1 = distance(point1, pointReference)
    val distance2 = distance(point2, pointReference)
    distance1 < distance2
  }

  def sortPoints(points: MatOfPoint2f, refer: Point): MatOfPoint2f = {
    val pointsSorted = points.toArray.sortWith((point1, point2) => {
      sortPointsWithReference(point1, point2, refer)
    })

    val subPointsSorted =
      Array(pointsSorted(1), pointsSorted(2)).sortWith(_.x < _.x)

    val x1y1 = pointsSorted(0)
    val x3y3 = pointsSorted(3)
    val x4y4 = subPointsSorted(0)
    val x2y2 = subPointsSorted(1)

    new MatOfPoint2f(x1y1, x2y2, x3y3, x4y4)
  }

  def getSizeOfQuad(points: MatOfPoint2f): (Double, Double) = {
    val arr    = points.toArray
    val width  = distance(arr(0), arr(1))
    val height = distance(arr(0), arr(3))
    (width, height)
  }

  def sortMatOfPoint(mop1: MatOfPoint, mop2: MatOfPoint): Boolean = {
    val contourArea1 = Imgproc.contourArea(mop1.t())
    val contourArea2 = Imgproc.contourArea(mop2.t())
    contourArea1 > contourArea2
  }

  def findSquare(list: List[MatOfPoint]): Option[MatOfPoint2f] = {
    var square: Option[MatOfPoint2f] = None
    breakable {
      for (c <- list) {
        val curve  = new core.MatOfPoint2f()
        val approx = new core.MatOfPoint2f()
        c.convertTo(curve, CvType.CV_32FC2)
        val peri = Imgproc.arcLength(curve, true)
        Imgproc.approxPolyDP(curve, approx, 0.02 * peri, true)
        if (approx.toList.size() == 4) {
          square = Some(approx)
          break()
        }
      }
    }
    square
  }

  def warpPerspectiveOperationOpt(mat: Mat): Option[Mat] = {
    val gray      = grayScaleOperation(mat)
    val blurred   = gaussianBlurOperation(gray)
    val edged     = cannyOperation(blurred)
    val dilated   = dilatationOperation(edged)
    val edgedCopy = new Mat(edged.rows(), edged.cols(), edged.`type`())
    edged.copyTo(edgedCopy)
    val contours = ListBuffer(List[MatOfPoint](): _*).asJava
    Imgproc.findContours(dilated,
                         contours,
                         edgedCopy,
                         Imgproc.RETR_EXTERNAL,
                         Imgproc.CHAIN_APPROX_SIMPLE)
    if (!contours.isEmpty) {
      val contoursSorted = contours.asScala.sortWith(sortMatOfPoint).toList
      val squareFound    = findSquare(contoursSorted)
      squareFound.map { square =>
        val pointsSorted = sortPoints(square, new Point(0, 0))
        val measures     = getSizeOfQuad(pointsSorted)
        val destImage =
          new Mat(measures._2.toInt, measures._1.toInt, mat.`type`())
        val dst_mat = new MatOfPoint2f(
          new Point(0, 0),
          new Point(destImage.width() - 1, 0),
          new Point(destImage.width() - 1, destImage.height() - 1),
          new Point(0, destImage.height() - 1)
        )
        val transform = Imgproc.getPerspectiveTransform(pointsSorted, dst_mat)
        Imgproc.warpPerspective(gray, destImage, transform, destImage.size())
        dilatationOperation(destImage)
      }
    } else None

  }

  def sortedGroups(group: List[MatOfPoint], t: Int): List[MatOfPoint] =
    group.sortWith((o1, o2) => {
      val rect1 = Imgproc.boundingRect(o1)
      val rect2 = Imgproc.boundingRect(o2)
      t match {
        case HORIZONTALLY => rect1.tl().x < rect2.tl().x
        case _            => rect1.tl().y < rect2.tl().y
      }
    })

  def filterContour(contour: MatOfPoint): Boolean = {
    val rect = Imgproc.boundingRect(contour)
    val ar   = rect.width / rect.height.toFloat
    rect.width >= 20 && rect.height >= 20 && ar >= 0.6 && ar <= 1.4
  }

  def evaluateBubble(bubble: MatOfPoint, closed: Mat): Int = {
    val mask = Mat.zeros(closed.size(), CvType.CV_8UC1)
    Imgproc.drawContours(
      mask,
      List(bubble).asJava,
      -1,
      new Scalar(255, 255, 255),
      -1
    )
    val totalBlankMax = (Core.countNonZero(mask) * 0.75).toInt
    Core.bitwise_and(closed, closed, mask, mask)
    val total = Core.countNonZero(mask)
    if (total >= totalBlankMax) 1
    else 0
  }

  def findContoursOperationOpt(matThreshold: Mat, typeSection: String): Option[List[List[Int]]] = {
    val out      = new Mat(matThreshold.rows(), matThreshold.cols(), CvType.CV_8UC3)
    val contours = ListBuffer(List[MatOfPoint](): _*).asJava
    val closed   = closeOperation(matThreshold)
    Imgproc.findContours(closed, contours, out, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
    val contoursList = contours.asScala.filter(filterContour).toList

    def process(groupLength: Int): List[List[Int]] = {
      val result = {
        if (typeSection == ANSWER) {
          sortedGroups(contoursList, VERTICALLY)
            .grouped(groupLength)
            .map(
              sortedGroups(_, HORIZONTALLY)
                .map(bubble => evaluateBubble(bubble, closed))
            )
        } else {
          sortedGroups(contoursList, HORIZONTALLY)
            .grouped(groupLength)
            .map(
              sortedGroups(_, VERTICALLY)
                .map(bubble => evaluateBubble(bubble, closed))
            )
        }
      }
      result.toList
    }

    val (quantity, groupDivide) =
      if (typeSection == ANSWER) (numberContoursOfAnswers, 20)
      else (numberContoursOfCodes, 10)

    if (contoursList.length == quantity) {
      Some(process(groupDivide))
    } else {
      println(
        s"Error : number of contours below limit ${contoursList.length} < $quantity"
      )
      None
    }
  }

  def qualifyTemplateOpt(img: Mat): Option[Exam] = {
    val dictionary = Aruco.getPredefinedDictionary(Aruco.DICT_4X4_50)
    val corners    = ListBuffer(List[Mat](): _*).asJava
    val ids        = new Mat()
    Aruco.detectMarkers(img, dictionary, corners, ids)
    if (!corners.isEmpty && corners.size() == 4) {
      val pointsWithCenter = getCornersTuple(ids, corners.asScala.toList)
        .flatMap(markWithCode => {
          val id = markWithCode._1.toInt
          Map(id -> getCenterSquare(id, markWithCode._2))
        })
        .toMap

      val codeMat =
        getSubMat(pointsWithCenter(CODE_12), pointsWithCenter(CODE_13), img)
      val alternativesMap =
        getSubMat(pointsWithCenter(ANSWER_42), pointsWithCenter(ANSWER_45), img)
      val codeContours = findContoursOperationOpt(
        thresholdOperation(codeMat),
        CODE
      )
      val alternativesContours = findContoursOperationOpt(
        thresholdOperation(alternativesMap),
        ANSWER
      )

      val codeResult         = codeContours.map(getCodeOfMatrix)
      val alternativesResult = alternativesContours.map(getAnswersOfMatrix)

      (codeResult, alternativesResult) match {
        case (Some(code), Some(alternatives)) =>
          Some(Exam(code, alternatives))
        case (None, Some(_)) =>
          println(s"impossible to qualify code")
          None
        case (Some(_), None) =>
          println(s"impossible to qualify alternatives")
          None
        case (None, None) =>
          println(s"impossible to qualify code and alternatives")
          None
      }
    } else None
  }

  def getCodeOfMatrix(matrix: List[List[Int]]): List[Answer] = {
    def f(group: List[Int]): Char = {
      val sumMarks = group.sum
      if (sumMarks == 0) 'y'
      else if (sumMarks == 1) s"${group.indexOf(1)}".charAt(0)
      else 'x'
    }
    val result = matrix.foldLeft((List.empty[Answer], 0)) { (acc, value) =>
      (acc._1 :+ Answer(acc._2, f(value), CODE), acc._2 + 1)
    }
    result._1.sortBy(_.index)
  }

  def getAnswersOfMatrix(matrix: List[List[Int]]): List[Answer] = {

    def f(group: List[Int], env: Map[Int, Char]): Char = {
      val sumMarks = group.sum
      if (sumMarks == 0) 'y'
      else if (sumMarks == 1) env(group.indexOf(1))
      else 'x'
    }

    matrix.par
      .foldLeft((List.empty[Answer], 1)) { (acc, value) =>
        val env   = Map(0 -> 'a', 1 -> 'b', 2 -> 'c', 3 -> 'd', 4 -> 'e')
        val chars = value.grouped(5).map(f(_, env)).toArray
        val answers = chars
          .foldLeft((List.empty[Answer], 0)) { (a, b) =>
            (a._1 :+ Answer(acc._2 + a._2, b, ANSWER), a._2 + 25)
          }
          ._1
        (acc._1 ++ answers, acc._2 + 1)
      }
      ._1
      .sortBy(_.index)
  }

  def getCornersTuple(ids: Mat, corners: List[Mat]): List[(Double, Mat)] =
    corners
      .foldLeft((List[(Double, Mat)](), 0)) { (result, value) =>
        val id    = ids.get(result._2, 0)(0)
        val index = result._2 + 1
        (result._1 :+ (id, value), index)
      }
      ._1

  def getCenterSquare(_id: Int, corner: Mat): (Int, Int) = {
    // size of mat = 4 x 1 => { (x1,y1) , (x2,y1) , (x2,y2) , (x1,y2) }
    val x1_y1 = (corner.get(0, 0)(0).toInt, corner.get(0, 0)(1).toInt)
    val x2_y2 = (corner.get(0, 2)(0).toInt, corner.get(0, 2)(1).toInt)
    _id match {
      case CODE_12   => x2_y2
      case CODE_13   => x1_y1
      case ANSWER_42 => x2_y2
      case _         => x1_y1
    }
  }

  def getSubMat(c1: (Int, Int), c2: (Int, Int), img: Mat): Mat =
    img.submat(c1._2, c2._2, c1._1, c2._1)

}
