package algorithm

import java.awt.image.BufferedImage
import java.io.{FileInputStream, InputStream}

import javax.imageio.ImageIO

import scala.util.{Failure, Success, Try}

object Util {

  def loadImage(inputStream: InputStream): BufferedImage = {
    val bufferedImage = ImageIO.read(inputStream)
    val width = bufferedImage.getWidth
    val height = bufferedImage.getHeight
    val img = new BufferedImage(width,height, BufferedImage.TYPE_INT_RGB)
    for( x <- 0 until width; y <- 0 until height ) img.setRGB( x, y, bufferedImage.getRGB(x,y))
    img
  }

  def loadFileImage(path: String): Option[BufferedImage] = {
   val fileInputStream = Try {
       new FileInputStream(path)
    }.toOption
    fileInputStream.map(loadImage(_))
  }

  def loadScalaImage(stream:InputStream): BufferedImage = {
    try {
      loadImage(stream)
    } finally {
      stream.close()
    }
  }

}
