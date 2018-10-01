package algorithm

import java.awt.image.BufferedImage

import scala.util.control.Breaks.{break, breakable}
import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer
import org.opencv.core._
import org.opencv.imgproc.Imgproc
import org.opencv.aruco.Aruco
import org.opencv.core

case class TransformError(error: String)

case class Result(success: Int, failures: Int)

object Algorithm {

  val CODE_10 = 10
  val CODE_11 = 11
  val ANSWER_20 = 20
  val ANSWER_21 = 21

  val CODE = "code"
  val ANSWER = "answer"

  def bufferedImageToMat(bufferedImage: BufferedImage): Mat = {
    val width = bufferedImage.getWidth()
    val height = bufferedImage.getHeight()
    val mat = bufferedImage.getType match {
      case BufferedImage.TYPE_INT_RGB =>
        val out = new Mat(bufferedImage.getHeight, bufferedImage.getWidth, CvType.CV_8UC3)
        val data = new Array[Byte](width * height * out.elemSize.asInstanceOf[Int])
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
        val data = new Array[Byte](width * height * out.elemSize.asInstanceOf[Int])
        val dataBuff = bufferedImage.getRGB(0, 0, width, height, null, 0, width)
        for ((x, i) <- dataBuff.view.zipWithIndex) {
          data(i) = (
            (0.21 * ((x >> 16) & 0xFF)) +
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
    val width = mat.cols()
    val height = mat.rows()
    val data = new Array[Byte](mat.rows * mat.cols * mat.elemSize.asInstanceOf[Int])
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

  def thresholdOperation(mat: Mat): Mat = {
    val out = new Mat(mat.rows(), mat.cols(), CvType.CV_8UC1)
    Imgproc.threshold(mat, out, 0, 255, Imgproc.THRESH_OTSU | Imgproc.THRESH_BINARY_INV)
    out
  }

  def dilatationOperation(mat: Mat): Mat = {
    val out = new Mat(mat.rows(), mat.cols(), CvType.CV_8UC1)
    Imgproc.dilate(mat, out, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2, 2)))
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

  def rotationWrap(mat: Mat) = {
    val out = mat.clone()
    val gray = grayScaleOperation(mat)
    val blurred = gaussianBlurOperation(gray)
    val edged = cannyOperation(blurred)
    val dilated = dilatationOperation(edged)
    val edgedCopy = new Mat(edged.rows(), edged.cols(), edged.`type`())
    var doCnt: Option[MatOfPoint2f] = None
    edged.copyTo(edgedCopy)
    val contours = ListBuffer(List[MatOfPoint](): _*).asJava
    Imgproc.findContours(dilated, contours, edgedCopy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
    if (contours.size() > 0) {
      val contoursSorted = contours.asScala.map(mop => (Imgproc.contourArea(mop.t()), mop)).sortWith(_._1 > _._1).map(_._2)
      breakable {
        for (c <- contoursSorted) {
          val curve = new core.MatOfPoint2f()
          val approx = new core.MatOfPoint2f()
          c.convertTo(curve, CvType.CV_32FC2)
          val peri = Imgproc.arcLength(curve, true)
          Imgproc.approxPolyDP(curve, approx, 0.02 * peri, true)
          if (approx.toList.size() == 4) {
            doCnt = Some(approx)
            break()
          }
        }
      }
    }


    if (doCnt.isDefined) {
      val destImage = mat.clone()
      val cnt = doCnt.get
      cnt.toList.asScala.foreach(println(_))
      val rect = Imgproc.minAreaRect(cnt).boundingRect()
      val x1_y1 = rect.tl()
      val x2_y2 = new Point(rect.br().x, rect.tl().y)
      val x3_y3 = rect.br()
      val x4_y4 = new core.Point(rect.tl().x, rect.br().y)
      val src_mat = new MatOfPoint2f(x1_y1, x2_y2, x3_y3, x4_y4)
      //val dst_mat = new MatOfPoint2f(new Point(0,0), new Point(destImage.width() - 1, 0), new Point(destImage.width() - 1, destImage.height() - 1), new Point(0, destImage.height() - 1))
      val dst_mat = new MatOfPoint2f(new Point(destImage.width() - 1, 0), new Point(0,0), new Point(0, destImage.height() - 1), new Point(destImage.width() - 1, destImage.height() - 1))
      val transform = Imgproc.getPerspectiveTransform(cnt, dst_mat)
      Imgproc.warpPerspective(mat, destImage, transform, out.size())
      destImage
    } else {
      mat
    }

  }

  def findContoursOperation(matThreshold: Mat, typeSection: String): List[List[Int]] = {
    val out = new Mat(matThreshold.rows(), matThreshold.cols(), CvType.CV_8UC3)
    val contours = ListBuffer(List[MatOfPoint](): _*).asJava
    Imgproc.findContours(matThreshold, contours, out, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

    val questionCntsList = contours.asScala.filter(c => {
      val rect = Imgproc.boundingRect(c)
      val ar = rect.width / rect.height.toFloat
      rect.width >= 20 && rect.height >= 20 && ar >= 0.9 && ar <= 1.1
    }).toList


    //sort by y coordinates using the topleft point of every contour's bounding box
    val sortedVertically = questionCntsList.sortWith((o1, o2) => {
      val rect1 = Imgproc.boundingRect(o1)
      val rect2 = Imgproc.boundingRect(o2)
      rect1.tl().y < rect2.tl().y
    })

    def sortedHorizontally(group: List[MatOfPoint]): List[MatOfPoint] = {
      group.sortWith((o1, o2) => {
        val rect1 = Imgproc.boundingRect(o1)
        val rect2 = Imgproc.boundingRect(o2)
        rect1.tl().x < rect2.tl().x
      })
    }

    def process(groupLength: Int): List[List[Int]] = {
      sortedVertically.grouped(groupLength).map(sortedHorizontally(_).map(buble => {
        val mask = Mat.zeros(matThreshold.size(), CvType.CV_8UC1)
        Imgproc.drawContours(mask, List(buble).asJava, -1, new Scalar(255, 255, 255), -1)
        Core.bitwise_and(matThreshold, matThreshold, mask, mask)
        val total = Core.countNonZero(mask)
        if (total > 500) 1
        else 0
      })).toList
    }

    println(s"number of circles of answers ${questionCntsList.length}")
    if (typeSection == ANSWER && questionCntsList.length == 500) {
      process(20)
    } else if (typeSection == CODE && sortedVertically.length == 60) {
      process(6)
    } else {
      // // otherwise we need to add a failure
      List[List[Int]]()
    }
  }


  def findAuroMarkers(grayImg: BufferedImage, colorImg: BufferedImage): (List[List[Int]], List[List[Int]]) = {
    val grayMat = bufferedImageToMat(grayImg)
    val colorMat = bufferedImageToMat(colorImg)
    val dictionary = Aruco.getPredefinedDictionary(Aruco.DICT_ARUCO_ORIGINAL)
    val corners = ListBuffer(List[Mat](): _*).asJava
    val ids = new Mat()
    Aruco.detectMarkers(grayMat, dictionary, corners, ids)
    if (corners.size() > 1) {
      // Aruco.drawDetectedMarkers(colorMat,corners,ids,new Scalar(0, 0, 255))
    } else {
      // we need to add exception or failure
    }
    val pointsWithCenter = getCornersTuple(ids, corners.asScala.toList).flatten { v => Map(v._1.toInt -> getCenterSquare(v._1.toInt, v._2)) }.toMap
    val codeMat = getSubMat(pointsWithCenter(CODE_10), pointsWithCenter(CODE_11), grayMat)
    val answerMat = getSubMat(pointsWithCenter(ANSWER_20), pointsWithCenter(ANSWER_21), grayMat)
    val codeResult = findContoursOperation(dilatationOperation(thresholdOperation(codeMat)), CODE)
    val answerResult = findContoursOperation(dilatationOperation(thresholdOperation(answerMat)), ANSWER)
    (codeResult, answerResult)
  }

  def getCornersTuple(ids: Mat, corners: List[Mat]): List[(Double, Mat)] = {
    corners.foldLeft((List[(Double, Mat)](), 0)) { (result, value) =>
      val id = ids.get(result._2, 0)(0)
      val index = result._2 + 1
      (result._1 :+ (id, value), index)
    }._1
  }


  def getCenterSquare(_id: Int, corner: Mat): (Int, Int) = {
    // size of mat = 4 x 1 => { (x1,y1) , (x2,y1) , (x2,y2) , (x1,y2) }
    val x1_y1 = (corner.get(0, 0)(0).toInt, corner.get(0, 0)(1).toInt)
    val x2_y2 = (corner.get(0, 2)(0).toInt, corner.get(0, 2)(1).toInt)
    _id match {
      case CODE_10 => x2_y2
      case CODE_11 => x1_y1
      case ANSWER_20 => x2_y2
      case _ => x1_y1
    }
  }

  def getSubMat(c1: (Int, Int), c2: (Int, Int), img: Mat): Mat = {
    img.submat(c1._2, c2._2, c1._1, c2._1)
  }

}

