package commons
import scala.collection.JavaConverters._
import models._
import org.opencv.core
import org.opencv.core.{ Core, CvType, Mat, MatOfPoint, MatOfPoint2f, Point, Scalar }
import org.opencv.imgproc.Imgproc

import scala.util.control.Breaks.{ break, breakable }

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

    matrix
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

  // _ < _
  def sortPointsWithReference(point1: Point, point2: Point, pointReference: Point): Boolean = {
    val distance1 = distance(point1, pointReference)
    val distance2 = distance(point2, pointReference)
    distance1 < distance2
  }

  def distance(p1: Point, p2: Point): Double = {
    val d1 = math.pow(p1.x - p2.x, 2)
    val d2 = math.pow(p1.y - p2.y, 2)
    math.sqrt(d1 + d2)
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

}
