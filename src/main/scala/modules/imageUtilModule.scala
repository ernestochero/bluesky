package modules
import java.awt.image.BufferedImage
import java.io.{ File, FileInputStream, IOException, InputStream }
import zio.{ Has, Task, UIO, ZIO, ZLayer }
import javax.imageio.ImageIO

package object imageUtilModule {
  type ImageUtilModule = Has[ImageUtilModule.Service]
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
      private def openStream(path: String): ZIO[Any, IOException, FileInputStream] =
        Task.effect(new FileInputStream(new File(path))).refineToOrDie[IOException]

      def getBufferedImage(path: String): ZIO[Any, Throwable, BufferedImage] =
        openStream(path).bracket(closeStream)(loadImage)
    }
    trait Service {
      def imageUtil: ZIO[ImageUtilModule, Throwable, ImageUtil]
    }
    val live: ZLayer.NoDeps[Nothing, ImageUtilModule] =
      ZLayer.succeed {
        new Service {
          override def imageUtil: ZIO[ImageUtilModule, Throwable, ImageUtil] =
            ZIO.succeed(ImageUtil())
        }
      }
  }

  def imageUtil: ZIO[ImageUtilModule, Throwable, ImageUtilModule.ImageUtil] =
    ZIO.accessM[ImageUtilModule](_.get.imageUtil)

}
