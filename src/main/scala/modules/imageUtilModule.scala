package modules
import java.awt.image.BufferedImage
import java.io.{ File, FileInputStream, IOException, InputStream }
import zio.{ Has, Task, UIO, ZIO, ZLayer, ZManaged }

import javax.imageio.ImageIO

package object imageUtilModule {
  type ImageUtilModule = Has[ImageUtilModule.Service]
  object ImageUtilModule {
    trait Service {
      def getBufferedImage(path: String): ZIO[Any, Throwable, BufferedImage]
    }
    val live: ZLayer.NoDeps[Nothing, ImageUtilModule] =
      ZLayer.succeed {
        new Service {

          private def closeStream(is: FileInputStream): UIO[Unit] = UIO.effectTotal(is.close())
          private def loadImage(inputStream: InputStream): UIO[BufferedImage] = {
            val bufferedImage = ImageIO.read(inputStream)
            val width         = bufferedImage.getWidth
            val height        = bufferedImage.getHeight
            val img           = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
            for (x <- 0 until width; y <- 0 until height)
              img.setRGB(x, y, bufferedImage.getRGB(x, y))
            ZIO.succeed(img)
          }
          private def openStream(path: String): ZIO[Any, IOException, FileInputStream] =
            Task.effect(new FileInputStream(new File(path))).refineToOrDie[IOException]

          override def getBufferedImage(path: String): ZIO[Any, Throwable, BufferedImage] =
            ZManaged.make(openStream(path))(closeStream).use(loadImage)
        }
      }
  }

  def getBufferedImage(path: String): ZIO[ImageUtilModule, Throwable, BufferedImage] =
    ZIO.accessM[ImageUtilModule](_.get.getBufferedImage(path))

}
