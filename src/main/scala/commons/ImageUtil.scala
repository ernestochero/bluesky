package commons
import java.io.File
object ImageUtil {
  def getPathsFromFolder(path: String): List[String] = {
    val file      = new File(path)
    val directory = if (file.isDirectory) Some(file) else None
    directory.fold(List.empty[String]) { folder =>
      val files = folder.listFiles().toList
      files.map(_.getPath)
    }
  }
}
