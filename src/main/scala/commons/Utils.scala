package commons

import java.awt.image.BufferedImage

import org.opencv.core.{ CvType, Mat, Size }
import org.opencv.imgproc.Imgproc

object Utils {

  implicit final class BufferedImageOps(private val bufferedImage: BufferedImage) extends AnyVal {
    def toMat: Mat = {
      val width  = bufferedImage.getWidth()
      val height = bufferedImage.getHeight()
      val mat = bufferedImage.getType match {
        case BufferedImage.TYPE_INT_RGB =>
          val out = new Mat(bufferedImage.getHeight, bufferedImage.getWidth, CvType.CV_8UC3)
          val data =
            new Array[Byte](width * height * out.elemSize.asInstanceOf[Int])
          val dataBuff = bufferedImage.getRGB(0, 0, width, height, null, 0, width)
          for ((x, i) <- dataBuff.view.zipWithIndex) {
            data(i * 3) = ((x >> 16) & 0xFF).asInstanceOf[Byte]
            data(i * 3 + 1) = ((x >> 8) & 0xFF).asInstanceOf[Byte]
            data(i * 3 + 2) = ((x >> 0) & 0xFF).asInstanceOf[Byte]
          }
          out.put(0, 0, data)
          out
        case _ =>
          val out = new Mat(bufferedImage.getHeight, bufferedImage.getWidth, CvType.CV_8UC1)
          val data =
            new Array[Byte](width * height * out.elemSize.asInstanceOf[Int])
          val dataBuff = bufferedImage.getRGB(0, 0, width, height, null, 0, width)
          for ((x, i) <- dataBuff.view.zipWithIndex) {
            data(i) = ((0.21 * ((x >> 16) & 0xFF)) +
            (0.71 * ((x >> 8) & 0xFF)) +
            (0.07 * ((x >> 0) & 0xFF)))
              .asInstanceOf[Byte]
          }
          out.put(0, 0, data)
          out
      }
      mat
    }
  }

  implicit final class MatOps(private val mat: Mat) extends AnyVal {
    def toBufferedImage: BufferedImage = {
      val width  = mat.cols()
      val height = mat.rows()
      val data =
        new Array[Byte](mat.rows * mat.cols * mat.elemSize.asInstanceOf[Int])
      mat.get(0, 0, data)
      val _type = mat.channels match {
        case 1 => BufferedImage.TYPE_BYTE_GRAY
        case _ => BufferedImage.TYPE_3BYTE_BGR
      }
      val bufferedImage = new BufferedImage(width, height, _type)
      bufferedImage.getRaster.setDataElements(0, 0, width, height, data)
      bufferedImage
    }

    def grayScaleOperation: Mat = {
      val out = new Mat(mat.rows(), mat.cols(), CvType.CV_8UC1)
      Imgproc.cvtColor(mat, out, Imgproc.COLOR_RGB2GRAY)
      out
    }

    // 165
    def thresholdOperation: Mat = {
      val out = new Mat(mat.rows(), mat.cols(), CvType.CV_8UC1)
      Imgproc.threshold(mat, out, 170, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C)
      out
    }

    def dilatationOperation: Mat = {
      val out = new Mat(mat.rows(), mat.cols(), CvType.CV_8UC1)
      Imgproc.dilate(
        mat,
        out,
        Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2, 2))
      )
      out
    }

    def erodeOperation: Mat = {
      val out = new Mat(mat.rows(), mat.cols(), CvType.CV_8UC1)
      Imgproc.erode(
        mat,
        out,
        Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(2, 2))
      )
      out
    }

    def gaussianBlurOperation: Mat = {
      val out = new Mat(mat.rows(), mat.cols(), CvType.CV_8UC1)
      Imgproc.GaussianBlur(mat, out, new Size(5, 5), 0)
      out
    }

    def cannyOperation: Mat = {
      val out = new Mat(mat.rows(), mat.cols(), CvType.CV_8UC1)
      Imgproc.Canny(mat, out, 75, 200)
      out
    }

    def openOperation: Mat = {
      val out = new Mat(mat.rows(), mat.cols(), CvType.CV_8UC1)
      Imgproc.morphologyEx(
        mat,
        out,
        Imgproc.MORPH_OPEN,
        Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(2, 2))
      )
      out
    }

    def closeOperation: Mat = {
      val out = new Mat(mat.rows(), mat.cols(), CvType.CV_8UC1)
      Imgproc.morphologyEx(
        mat,
        out,
        Imgproc.MORPH_CLOSE,
        Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(4, 4))
      )
      out
    }
  }
}
