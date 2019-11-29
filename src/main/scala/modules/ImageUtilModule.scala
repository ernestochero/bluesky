package modules
import java.awt.image.BufferedImage
import java.io.{ File, FileInputStream, InputStream }

import zio.{ Task, UIO, URIO, ZIO }
import ImageUtilModule._
import javax.imageio.ImageIO
trait ImageUtilModule {
  val imageUtilModule: Service[Any]
}
object ImageUtilModule {
  case class ImageUtil() {
    private def closeStream(is: FileInputStream): UIO[Unit] =
      UIO.effectTotal(is.close())
    private def loadImage(inputStream: InputStream): UIO[BufferedImage] = {
      val bufferedImage = ImageIO.read(inputStream)
      val width         = bufferedImage.getWidth
      val height        = bufferedImage.getHeight
      val img           = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
      for (x <- 0 until width; y <- 0 until height) img.setRGB(x, y, bufferedImage.getRGB(x, y))
      ZIO.succeed(img)
    }

    def getBufferedImage(path: String): URIO[Any, BufferedImage] =
      Task
        .effect(new FileInputStream(new File(path)))
        .bracket(closeStream)(loadImage)
        .orDie
  }
  trait Service[R] {
    def imageUtil: ZIO[R, Throwable, ImageUtil]
  }

  trait Live extends ImageUtilModule {
    override val imageUtilModule: Service[Any] = new Service[Any] {
      override def imageUtil: ZIO[Any, Throwable, ImageUtil] = ZIO.succeed(ImageUtil())
    }
  }

  object factory extends Service[ImageUtilModule] {
    override def imageUtil: ZIO[ImageUtilModule, Throwable, ImageUtil] =
      ZIO.accessM[ImageUtilModule](
        _.imageUtilModule.imageUtil
      )
  }

}
