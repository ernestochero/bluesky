package algorithm
import java.awt.Graphics
import java.awt.image.BufferedImage
import java.io.{FileInputStream, InputStream, File}

import javax.imageio.ImageIO
import javax.swing.JComponent

class PhotoCanvas extends JComponent {
  var imagePath: Option[String] = None
  val stream = this.getClass.getResourceAsStream("/exams/empty.png")

  var image = loadScalaImage(stream)

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


  def uploadPattern(path: String):Unit = {
    val image  = loadFileImage(path)
    Algorithm.warpPerspectiveOperation(Algorithm.bufferedImageToMat(image)) match {
      case Right(result) =>
        Algorithm.calificateTemplate(result) match {
          case Right(result) =>
            val (code,answers) = result
            code.foreach(println(_))
            answers.sortBy(_.index).foreach(println(_))
          case Left(failure) => println(failure.error)
        }
        this.image = Algorithm.matToBufferedImage(result)
      case Left(failure) => println(failure.error)
    }
  }



  def uploadExams(paths: Array[String]): Unit = {
    val t0 = System.nanoTime()
    var results = Array.empty[(List[Answer], List[Answer])]
    var i = 1

    paths.foreach( path => {
      Algorithm.warpPerspectiveOperation(Algorithm.bufferedImageToMat( loadFileImage(path) )) match {
        case Right(value) => {
          Algorithm.calificateTemplate(value) match {
            case Right(result) => {
              results = results :+ result
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
    println(s"Total of Exams Analyzed  = ${results.length}")
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
