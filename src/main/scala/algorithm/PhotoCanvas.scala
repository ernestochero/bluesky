package algorithm
import java.awt.Graphics
import java.awt.image.BufferedImage
import java.io.{File, FileInputStream, InputStream}

import algorithm.Algorithm.{ANSWER, ANSWER_42, ANSWER_45, CODE, CODE_12, CODE_13, findContoursOperation, getCenterSquare, getCornersTuple, getSubMat, thresholdOperation}

import scala.collection.JavaConverters._
import javax.imageio.ImageIO
import javax.swing.JComponent
import org.opencv.aruco.Aruco
import org.opencv.core.Mat

import scala.collection.mutable.ListBuffer

class PhotoCanvas extends JComponent {
  var imagePath: Option[String] = None
  val stream = this.getClass.getResourceAsStream("/exams/empty.png")

  var image = loadScalaImage(stream)

  var pattern = (List.empty[Answer], List.empty[Answer])
  var examsResult =  Array.empty[(List[Answer], List[Answer])]
  var qualifyResults = Array.empty[(String, Int)]

  private def loadScalaImage(stream:InputStream): BufferedImage = {
    try {
      loadImage(stream)
    } finally {
      stream.close()
    }
  }

  private def loadFileImage(path: String): BufferedImage = {
    val stream = new FileInputStream(path)
    try {
      loadImage(stream)
    } finally {
      stream.close()
    }
  }

  private def loadImage(inputStream: InputStream): BufferedImage = {
    val bufferedImage = ImageIO.read(inputStream)
    val width = bufferedImage.getWidth
    val height = bufferedImage.getHeight
    val img = new BufferedImage(width,height, BufferedImage.TYPE_INT_RGB)
    for( x <- 0 until width; y <- 0 until height ) img.setRGB( x, y, bufferedImage.getRGB(x,y))
    img
  }

  def reload(): Unit = {
    image = imagePath match {
      case Some(path) => loadFileImage(path)
      case None => loadScalaImage(stream)
    }
    repaint()
  }

  def loadFile(path: String): Unit = {
    imagePath = Some(path)
    reload()
  }

  def applyQualify(): Unit = {
    this.qualifyResults = qualify(examsResult, pattern)
    this.qualifyResults.foreach { p =>
      println(s"Code : ${p._1} ---> final result : ${p._2}" )
    }
  }

  def qualify(exams:Array[(List[Answer], List[Answer])], pattern: (List[Answer], List[Answer])) = {
    def compare(ec:Char, pc:Char):Int = if ( ec == pc ) 1 else 0
    def f( e:(List[Answer], List[Answer]), p:(List[Answer], List[Answer]) ):(String,Int) = {
      val code = e._1.map(_.value).mkString("")
      // val answers = e._2.map(c => (c.index, c.value))
      val total = e._2.map(_.value).zip(p._2.map(_.value)).map(c => compare(c._1,c._2)).sum
      (code, total)
    }

    def showsValuesRecognize(exam:(List[Answer],List[Answer])): Unit = {
      val (code, alternatives) = exam
      println(code.map(_.value).mkString(""))
      alternatives.foreach(a => println(s"${a.value}"))
    }
    // that's a bad pattern but I just do for test purpose
    //showsValuesRecognize(exam)
    exams.map(exam => {
      f(exam,pattern)
    })
  }

  def uploadPattern(path: String):Unit = {
    val image  = loadFileImage(path)
    val matImage = Algorithm.bufferedImageToMat(image)
    Algorithm.warpPerspectiveOperation(matImage) match {
      case Right(img) =>
        Algorithm.calificateTemplate(img) match {
          case Right(result) => this.pattern = result
          case Left(failure) => println(failure.error)
        }
        this.image = Algorithm.matToBufferedImage(img)
      case Left(failure) => println(failure.error)
    }
  }

  def uploadExams(paths: Array[String]): Unit = {
    val t0 = System.nanoTime()
    var i = 1

    paths.foreach( path => {
      Algorithm.warpPerspectiveOperation(Algorithm.bufferedImageToMat( loadFileImage(path) )) match {
        case Right(value) => {
          Algorithm.calificateTemplate(value) match {
            case Right(result) => {
              this.examsResult = this.examsResult :+ result
              println(s"Done Exam ${i}")
              i = i + 1
            }
            case Left(failure) => println(s"Error : ${failure}")
          }
        }
        case Left(failure) => println(s"Error : ${failure}")
      }
    })
    val t1 = System.nanoTime()
    println("Elapsed time: " + (t1 - t0) + "ns")
    println(s"Total of Exams Analyzed  = ${this.examsResult.length}")
  }


 /* printToFile(new java.io.File(s"$strCode.txt")) { p =>
    answers.foreach(p.println)
  }
  */
  def printToFile(f: java.io.File)(op: java.io.PrintWriter => Unit) {
    val p = new java.io.PrintWriter(f)
    try { op(p) } finally { p.close() }
  }

  def applyRotateOperation(): Unit = {
    val img = this.image
    /*val m = Algorithm.matToBufferedImage(Algorithm.warpPerspectiveOperation2(Algorithm.bufferedImageToMat(img)))
    this.image = m
    repaint()*/
    Algorithm.warpPerspectiveOperation(Algorithm.bufferedImageToMat(img)) match {
      case Right(result) =>
        this.image = Algorithm.matToBufferedImage(result)
        repaint()
      case Left(failure) => println(failure.error)
    }
  }

  def testAlgorithmImages(path:String): Unit = {
    val image = loadFileImage(path)
    val matImage = Algorithm.bufferedImageToMat(image)
    val dictionary = Aruco.getPredefinedDictionary(Aruco.DICT_4X4_50)
    val corners = ListBuffer(List[Mat](): _*).asJava
    val ids = new Mat()
    // warpPerspective
    Algorithm.warpPerspectiveOperation(matImage) match {
      case Right(img) =>
        Aruco.detectMarkers(img, dictionary, corners, ids)
        println(corners.size())

        val threshold = Algorithm.thresholdOperation(img)
        val close = Algorithm.closeOperantion(threshold)
        this.image = Algorithm.matToBufferedImage(close)

        val pointsWithCenter = getCornersTuple(ids, corners.asScala.toList).flatten { v => Map(v._1.toInt -> getCenterSquare(v._1.toInt, v._2)) }.toMap
        val codeMat = getSubMat(pointsWithCenter(CODE_12),pointsWithCenter(CODE_13), img)
        val answerMat = getSubMat(pointsWithCenter(ANSWER_42), pointsWithCenter(ANSWER_45), img)
        val Right(codeResult) = findContoursOperation(thresholdOperation(codeMat), CODE)
        val Right(answerResult) = findContoursOperation(thresholdOperation(answerMat), ANSWER)
      case Left(failure) => println(failure.error)
    }

    /*
    val gray = Algorithm.grayScaleOperation(matImage)
    val threshold = Algorithm.thresholdOperation(gray)
    val open = Algorithm.openOperantion(threshold)
    val firstDilatation = Algorithm.dilatationOperation(open)*/

  }

  def applyGrayScaleOperation(): Unit = {
    /*val m = Algorithm.matToBufferedImage(Algorithm.findContoursOperation2(Algorithm.bufferedImageToMat(this.image)))
    // val m = Algorithm.matToBufferedImage(Algorithm.drawArucoMarkers(Algorithm.bufferedImageToMat(this.image)))
    this.image = m
    repaint()*/
    val img = Algorithm.bufferedImageToMat(this.image)
    Algorithm.calificateTemplate(img) match {
      case Right(result) =>
        val (code,answers) = result
        code.foreach(println(_))
        answers.sortBy(_.index).foreach(println(_))
      case Left(failure) => println(failure.error)
    }
  }

  override def paintComponent(graphics: Graphics): Unit = {
    super.paintComponent(graphics)
    graphics.drawImage(image, 30, 30, this.image.getWidth/2 , this.image.getHeight/2 ,null)
  }
}
