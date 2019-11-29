package commons

import java.awt.image.BufferedImage
import java.io.{ File, FileInputStream, InputStream }

import javax.imageio.ImageIO

import scala.util.Try

object ImageUtil {

  def loadImage(inputStream: InputStream): BufferedImage = {
    val bufferedImage = ImageIO.read(inputStream)
    val width         = bufferedImage.getWidth
    val height        = bufferedImage.getHeight
    val img           = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    for (x <- 0 until width; y <- 0 until height) img.setRGB(x, y, bufferedImage.getRGB(x, y))
    img
  }

  def createInputStream(path: String): Option[InputStream] =
    Try {
      new FileInputStream(path)
    }.toOption

  def loadFileImage(path: String): Option[BufferedImage] = {
    val fileInputStream = createInputStream(path)
    fileInputStream.map(loadImage)
  }

  def getPathsFromFolder(path: String): List[String] = {
    val file      = new File(path)
    val directory = if (file.isDirectory) Some(file) else None
    directory.fold(List.empty[String]) { folder =>
      val files = folder.listFiles().toList
      files.map(_.getPath)
    }
  }

}
