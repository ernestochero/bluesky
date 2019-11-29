package models

import org.opencv.core.Mat
case class Answer(index: Int, value: Char, category: String)
case class Exam(code: List[Answer], alternatives: List[Answer])
case class SizeM(width: Double, height: Double)
case class Mark(id: Int, mat: Mat)
