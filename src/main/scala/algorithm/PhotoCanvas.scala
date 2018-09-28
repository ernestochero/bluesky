package algorithm
import java.awt.Graphics
import java.awt.image.BufferedImage
import java.io.{FileInputStream, InputStream}
import Algorithm.{grayScaleOperation, thresholdOperation, findContoursOperation, dilatationOperation, findAuroMarkers}

import javax.imageio.ImageIO
import javax.swing.JComponent

class PhotoCanvas extends JComponent {
  var imagePath: Option[String] = None
  val stream = this.getClass.getResourceAsStream("/exams/examFM.png")

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


  def applyGrayScaleOperation(): Unit = {
    val originalImg = this.image
    // this.image = findContoursOperation(dilatationOperation(thresholdOperation(grayScaleOperation(this.image))),originalImg)
    this.image = findAuroMarkers(grayScaleOperation(this.image),originalImg)
    /*this.image = thresholdOperation(grayScaleOperation(this.image))*/
    repaint()
  }

  override def paintComponent(graphics: Graphics): Unit = {
    super.paintComponent(graphics)
    graphics.drawImage(image, 100, 10, this.image.getWidth/2 , this.image.getHeight/2 ,null)
  }
}
