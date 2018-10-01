package algorithm
import java.awt.Graphics
import java.awt.image.BufferedImage
import java.io.{FileInputStream, InputStream}
import Algorithm._

import javax.imageio.ImageIO
import javax.swing.JComponent

class PhotoCanvas extends JComponent {
  var imagePath: Option[String] = None
  val stream = this.getClass.getResourceAsStream("/exams/examScannedWrong-2.png")

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

  def applyRotateOperation(): Unit = {
    val img = this.image
    Algorithm.rotationWrap(Algorithm.bufferedImageToMat(img)) match {
      case Right(result) =>
        this.image = Algorithm.matToBufferedImage(result)
        repaint()
      case Left(error) => error
    }
  }

  def applyGrayScaleOperation(): Unit = {
    val img = Algorithm.bufferedImageToMat(this.image)
    println("heyyy")
    Algorithm.calificateTemplate(img, img) match {
      case Right(result) =>
        val (a,b) = result
        a.foreach(c => println(c.mkString(" ")))
        println("heyyy")
        b.foreach(c => println(c.mkString(" ")))
      case Left(x) => println(x.error)
    }

  }

  override def paintComponent(graphics: Graphics): Unit = {
    super.paintComponent(graphics)
    graphics.drawImage(image, 100, 10, this.image.getWidth/2 , this.image.getHeight/2 ,null)
  }
}
