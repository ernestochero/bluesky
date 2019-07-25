package algorithm

import java.awt.image.BufferedImage
import java.io._
import javax.imageio.ImageIO

import scala.util.Try

object Util {

  def loadImage(inputStream: InputStream): BufferedImage = {
    val bufferedImage = ImageIO.read(inputStream)
    val width = bufferedImage.getWidth
    val height = bufferedImage.getHeight
    val img = new BufferedImage(width,height, BufferedImage.TYPE_INT_RGB)
    for( x <- 0 until width; y <- 0 until height ) img.setRGB( x, y, bufferedImage.getRGB(x,y))
    img
  }

  def createInputStream(path: String): Option[InputStream] = {
    Try {
      new FileInputStream(path)
    }.toOption
  }

  def loadFileImage(path: String): Option[BufferedImage] = {
   val fileInputStream = createInputStream(path)
    fileInputStream.map(loadImage)
  }

  def loadFilesImageFromFolder(path: String): Option[List[Option[BufferedImage]]] = {
    val file = new File(path)
    val folder = if (file.isDirectory) Some(file) else None
    println(s"### ${folder}")
    val files = folder.map(_.listFiles().toList)
    val filesPath = files.map(_.map(_.getPath))
    val filesInputStream = filesPath.map(_.map( path => {
      val inputStream = createInputStream(path)
      inputStream.map(loadImage)
    }))
    filesInputStream
  }

  def loadScalaImage(stream:InputStream): BufferedImage = {
    try {
      loadImage(stream)
    } finally {
      stream.close()
    }
  }

}
