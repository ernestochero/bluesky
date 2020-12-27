package commons
import zio.{ Task, ZIO }

import java.io.{ File, IOException }
object ImageUtil {
  def getPathsFromFolder(path: String): ZIO[Any, IOException, List[String]] =
    Task.effect(listPaths(new File(path))).refineToOrDie[IOException]

  def listPaths(file: File): List[String] = {
    val directory = if (file.isDirectory) Some(file) else None
    directory.fold(List.empty[String]) { folder =>
      val files = folder.listFiles().toList
      files.map(_.getPath)
    }
  }
}
