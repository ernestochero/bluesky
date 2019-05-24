package algorithm

import java.awt.image.BufferedImage

import scala.util.control.Breaks.{break, breakable}
import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer
import org.opencv.core._
import org.opencv.imgproc.Imgproc
import org.opencv.aruco.Aruco
import org.opencv.core

import scala.util.Try
import scala.util.{Success}

case class TransformError(error: String)

case class Result(success: Int, failures: Int)

case class Answer(index:Int, value:Char, category:String)

object Algorithm {

  val CODE_12 = 12
  val CODE_13 = 13
  val ANSWER_42 = 42
  val ANSWER_45 = 45

  val CODE = "code"
  val ANSWER = "answer"

  val numberContoursOfCodes = 70
  val numberContoursOfAnswers = 500

  val HORIZONTALLY = 1
  val VERTICALLY = 2

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
// 165
  def thresholdOperation(mat: Mat): Mat = {
    val out = new Mat(mat.rows(), mat.cols(), CvType.CV_8UC1)
    Imgproc.threshold(mat, out, 170, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C)
    out
  }

  def dilatationOperation(mat: Mat): Mat = {
    val out = new Mat(mat.rows(), mat.cols(), CvType.CV_8UC1)
    Imgproc.dilate(mat, out, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2, 2)))
    out
  }

  def erodeOperation(mat:Mat): Mat = {
    val out = new Mat(mat.rows(), mat.cols(), CvType.CV_8UC1)
    Imgproc.erode(mat, out, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(2, 2)))
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

  def openOperantion(mat: Mat): Mat = {
    val out = new Mat(mat.rows(), mat.cols(), CvType.CV_8UC1)
    Imgproc.morphologyEx(mat, out, Imgproc.MORPH_OPEN, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(2, 2)))
    out
  }

  def closeOperantion(mat: Mat): Mat = {
    val out = new Mat(mat.rows(), mat.cols(), CvType.CV_8UC1)
    Imgproc.morphologyEx(mat, out, Imgproc.MORPH_CLOSE, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(4, 4)))
    out
  }

  def distance(p1:Point, p2:Point): Double = {
    val d1 = math.pow(p1.x - p2.x,2)
    val d2 = math.pow(p1.y - p2.y,2)
    math.sqrt(d1 + d2)
  }

  def sortPoints(points:MatOfPoint2f,refer:Point): MatOfPoint2f = {
    val arr = points.toArray.map(point => {
      (distance(refer,point),point)
    }).sortWith(_._1 < _._1)

    val x1_y1 = arr.head._2
    val x3_y3 = arr.last._2
    val subArr = Array(arr(1),arr(2)).sortWith(_._2.x < _._2.x)
    val x4_y4 = subArr.head._2
    val x2_x2 = subArr.last._2
    new MatOfPoint2f(x1_y1, x2_x2, x3_y3, x4_y4)
  }

  def getSizeOfQuad(points:MatOfPoint2f):(Double, Double) = {
    val arr = points.toArray
    val width = distance(arr(0), arr(1))
    val height = distance(arr(0), arr(3))
    (width, height)
  }

  def warpPerspectiveOperation2(mat: Mat): Mat = {
    val gray = grayScaleOperation(mat)
    val blurred = gaussianBlurOperation(gray)
    val edged = cannyOperation(blurred)
    val dilated = dilatationOperation(edged)
    val edgedCopy = new Mat(edged.rows(), edged.cols(), edged.`type`())
    dilated
  }

  def warpPerspectiveOperation(mat: Mat): Either[TransformError, Mat] = {
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

    doCnt match {
      case Some(cnt) =>
        val cntOrdered = sortPoints(cnt, new Point(0,0))
        val measures = getSizeOfQuad(cntOrdered)
        val destImage = new Mat(measures._2.toInt, measures._1.toInt, mat.`type`())
        val dst_mat = new MatOfPoint2f(new Point(0,0), new Point(destImage.width() - 1, 0), new Point(destImage.width() - 1, destImage.height() - 1), new Point(0, destImage.height() - 1))
        val transform = Imgproc.getPerspectiveTransform(cntOrdered, dst_mat)
        Imgproc.warpPerspective(gray, destImage, transform, destImage.size())
        val destImageFinal = dilatationOperation(destImage)
        Right(destImageFinal)

      case _ =>
        Left(TransformError("Impossible to get contour to apply warpPerspective"))
    }

  }

  def sortedGroups(group: List[MatOfPoint], t: Int): List[MatOfPoint] = {
    group.sortWith((o1, o2) => {
      val rect1 = Imgproc.boundingRect(o1)
      val rect2 = Imgproc.boundingRect(o2)
      t match {
        case HORIZONTALLY => rect1.tl().x < rect2.tl().x
        case _ => rect1.tl().y < rect2.tl().y
      }
    })
  }

  def findContoursOperation(matThreshold: Mat, typeSection: String): Either[TransformError,List[List[Int]]] = {
    val out = new Mat(matThreshold.rows(), matThreshold.cols(), CvType.CV_8UC3)
    val contours = ListBuffer(List[MatOfPoint](): _*).asJava
    val closed = closeOperantion(matThreshold)
    Imgproc.findContours(closed, contours, out, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
    println(s"TYPE : ${typeSection}")
    val contoursList = contours.asScala.filter(c => {
      val rect = Imgproc.boundingRect(c)

      val ar = rect.width / rect.height.toFloat
     /* println(rect.width + " " + rect.height + " " + ar)*/
      rect.width >= 20 && rect.height >= 20 && ar >= 0.6 && ar <= 1.4
    }).toList

    def process(groupLength: Int): List[List[Int]] = {
      val list = sortedGroups(contoursList, VERTICALLY).grouped(groupLength).map(sortedGroups(_,HORIZONTALLY).map(bubble => {
        val mask = Mat.zeros(closed.size(), CvType.CV_8UC1)
        Imgproc.drawContours(mask, List(bubble).asJava, -1, new Scalar(255, 255, 255), -1)
        Core.bitwise_and(closed, closed, mask, mask)
        val total = Core.countNonZero(mask)
        if (total >= 280) 1
        else if ( total >= 210 ) 2
        else 0
      })).toList

     /* list.zipWithIndex.foreach{c =>
        print(s"row ${c._2} -> ")
        c._1.grouped(5).foreach{ x =>
          print(s"${x.mkString(" ")} - ")
        }
        println("")
      }*/

      list

    }

    println(s"number of circles of answers ${contoursList.length}")
    val (quantity, groupDivide) = if(typeSection == ANSWER) (numberContoursOfAnswers, 20) else (numberContoursOfCodes, 7)
    if ( contoursList.length == quantity ) {
      Right(process(groupDivide))
    } else {
      Left(TransformError(s"Error : number of contours below limit ${contoursList.length} < $quantity"))
    }
  }

  def drawArucoMarkers(grayMat: Mat): Mat = {
    val dictionary = Aruco.getPredefinedDictionary(Aruco.DICT_4X4_50)
    val corners = ListBuffer(List[Mat](): _*).asJava
    val ids = new Mat()
    Aruco.detectMarkers(grayMat, dictionary, corners, ids)
    Aruco.drawDetectedMarkers(grayMat,corners,ids,new Scalar(0, 0, 255))
    grayMat
  }

  def calificateTemplate(grayMat: Mat): Either[TransformError,(List[Answer], List[Answer])] = {
    val dictionary = Aruco.getPredefinedDictionary(Aruco.DICT_4X4_50)
    val corners = ListBuffer(List[Mat](): _*).asJava
    val ids = new Mat()
    Aruco.detectMarkers(grayMat, dictionary, corners, ids)
    println(corners.size())
    if(!corners.isEmpty && corners.size() == 4) {
      val pointsWithCenter = getCornersTuple(ids, corners.asScala.toList).flatten { v => Map(v._1.toInt -> getCenterSquare(v._1.toInt, v._2)) }.toMap
      val codeMat = getSubMat(pointsWithCenter(CODE_12),pointsWithCenter(CODE_13), grayMat)
      val answerMat = getSubMat(pointsWithCenter(ANSWER_42), pointsWithCenter(ANSWER_45), grayMat)
      val Right(codeResult) = findContoursOperation(thresholdOperation(codeMat), CODE)
      val Right(answerResult) = findContoursOperation(thresholdOperation(answerMat), ANSWER)
      Right((getCodeofMatrix(codeResult), getAnswersofMatrix(answerResult)))
    }
    else Left(TransformError("Corners are Empty, don't find the Exact Aruco Markers"))
  }

  def drawdraw(grayMat:Mat): Mat = {
    val dictionary = Aruco.getPredefinedDictionary(Aruco.DICT_ARUCO_ORIGINAL)
    val corners = ListBuffer(List[Mat](): _*).asJava
    val ids = new Mat()
    Aruco.detectMarkers(grayMat, dictionary, corners, ids)
    if(!corners.isEmpty && corners.size() == 4) {
      findContoursOperation2(grayMat)
      /*val pointsWithCenter = getCornersTuple(ids, corners.asScala.toList).flatten { v => Map(v._1.toInt -> getCenterSquare(v._1.toInt, v._2)) }.toMap
      val codeMat = getSubMat(pointsWithCenter(CODE_12),pointsWithCenter(CODE_13), grayMat)
      val answerMat = getSubMat(pointsWithCenter(ANSWER_42), pointsWithCenter(ANSWER_45), grayMat)
      val Right(codeResult) = findContoursOperation(dilatationOperation(thresholdOperation(codeMat)), CODE)
      val Right(answerResult) = findContoursOperation(dilatationOperation(thresholdOperation(answerMat)), ANSWER)
      Right((getCodeofMatrix(codeResult), getAnswersofMatrix(answerResult)))*/
    } else {
      grayMat
    }
  }

  def findContoursOperation2(grayMat: Mat): Mat = {
    val dictionary = Aruco.getPredefinedDictionary(Aruco.DICT_4X4_50)
    val corners = ListBuffer(List[Mat](): _*).asJava
    val ids = new Mat()
    Aruco.detectMarkers(grayMat, dictionary, corners, ids)
    println(s"corners ${corners.size()}")
    if(!corners.isEmpty && corners.size() == 4) {

      val pointsWithCenter = getCornersTuple(ids, corners.asScala.toList).flatten { v => Map(v._1.toInt -> getCenterSquare(v._1.toInt, v._2)) }.toMap
      val codeMat = getSubMat(pointsWithCenter(CODE_12),pointsWithCenter(CODE_13), grayMat)
      val answerMat = getSubMat(pointsWithCenter(ANSWER_42), pointsWithCenter(ANSWER_45), grayMat)

      val matThreshold_x = thresholdOperation(answerMat)
      val open = openOperantion(matThreshold_x)
      val closed = closeOperantion(matThreshold_x)
      val out = new Mat(matThreshold_x.rows(), matThreshold_x.cols(), CvType.CV_8UC3)
      val contours = ListBuffer(List[MatOfPoint](): _*).asJava

      Imgproc.findContours(closed, contours, out, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
      println(s"contours initial  : ${contours.size()}")
      val contoursList = contours.asScala.filter(c => {
        val rect = Imgproc.boundingRect(c)
        val ar = rect.width / rect.height.toFloat
/*        if (rect.width >= 20 && rect.height >= 20 && ar >= 0.8 && ar <= 1.2) println(rect.width + " " + rect.height + " " + ar)*/
        rect.width >= 20 && rect.height >= 20 && ar >= 0.6 && ar <= 1.4
      }).asJava

      println(s"contours ${contoursList.size}")

      //Imgproc.drawContours(codeMat, contoursList, -1, new Scalar(255, 255, 255), -1 )

      /*val s = openOperantion(closed)
      openOperantion(s)*/
      val groupLength = 7

      val mask = Mat.zeros(matThreshold_x.size(), CvType.CV_8UC1)

      /*sortedGroups(contoursList.asScala.toList, VERTICALLY).grouped(groupLength).toList.foreach(g1 => {
        sortedGroups(g1,HORIZONTALLY).foreach(b => {
          Imgproc.drawContours(mask, List(b).asJava, -1, new Scalar(255, 255, 255), -1)
          Core.bitwise_and(closed, closed, mask, mask)
        })
      })
*/

      // Core.bitwise_and(matThreshold_x, matThreshold_x, mask, mask)

      // val total = Core.countNonZero(mask)

      closed

    } else {
      grayMat
    }
  }

  def getCodeofMatrix(matrix: List[List[Int]]): List[Answer] = {

    def f(group:List[Int],answers:List[Answer], number: Int): (List[Answer], Int) = {
      if (answers.isEmpty) {
        val code = group.foldLeft((answers,0)) { (acc, value) =>
          if (value == 0) (acc._1 :+ Answer(acc._2, 'e', CODE), acc._2 + 1)
          else ( acc._1 :+ Answer(acc._2, number.toString.charAt(0), CODE), acc._2 + 1)
        }._1

        (code, number + 1)

      } else {
        val code = answers.foldLeft(List.empty[Answer]) {(acc, answer) =>
          if ((group(answer.index) != 0 && answer.value != 'e') || (group(answer.index) == 2 && answer.value == 'e')) acc :+ answer.copy(value = 'x')
          else if (group(answer.index) == 1 && answer.value == 'e') acc :+ answer.copy(value = number.toString.charAt(0))
          else acc :+ answer
        }
        (code, number + 1)
      }
    }

    matrix.foldLeft((List.empty[Answer],0)){(tuple,group) => f(group,tuple._1,tuple._2)}._1
  }

  def getAnswersofMatrix(matrix: List[List[Int]]): List[Answer] = {

    def f(group:List[Int], env:Map[Int,Char]):Char = {
      val countG = group.groupBy(identity).mapValues(_.length).filterKeys(_ != 0)
      if(countG.size == 1 ) {
        Try(countG(1)) match {
          case Success(value) if value == 1 => env(group.indexOf(1))
          case _ => 'x'
        }
      } else 'x'
    }

    matrix.par.foldLeft((List.empty[Answer],1)) { (acc, value) =>
      val env = Map(0 -> 'a', 1 ->'b',2 -> 'c', 3 ->'d',4 -> 'e')
      val chars = value.grouped(5).map(f(_,env)).toArray
      val answers = chars.foldLeft((List.empty[Answer],0)) { (a,b) =>
        (a._1 :+ Answer(acc._2 + a._2, b, ANSWER), a._2 + 25)
      }._1
      (acc._1 ++ answers, acc._2 + 1)
    }._1.sortBy(_.index)
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
      case CODE_12 => x2_y2
      case CODE_13 => x1_y1
      case ANSWER_42 => x2_y2
      case _ => x1_y1
    }
  }


  def getSubMat(c1: (Int, Int), c2: (Int, Int), img: Mat): Mat = {
    img.submat(c1._2, c2._2, c1._1, c2._1)
  }

}

