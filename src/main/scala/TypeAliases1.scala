object TypeAliases1 extends App {
  type Row = List[Int]
  def Row(xs: Int*) = List(xs: _*)

  type Matrix = List[Row]
  def Matrix(xs: Row*) = List(xs: _*)

  val m = Matrix(
    Row(1,2,3),
    Row(1,2,3),
    Row(1,2,3))

  println(m)
  println(m.getClass)

  val times: Int => Int = x => x * 2
  println(times(3))

  def times2(x: Int): Int = x * 2
  val z = times2 _

  def f(n: Int, z: Int => Int) : Int = {
    z(n)
  }

  println(f(4, times2))
  println(f(6, z))

  trait Str { def str: String }
  trait Count { def count: Int }

  def repeat(cd: Str with Count): String = {
    Iterator.fill(cd.count)(cd.str).mkString
  }

  val sc = new Str with Count {
    val str = "test"
    val count = 3
  }

  val ca  = new Count with Str {
    val str = "ernesto"
    val count = 4
  }
  println(repeat(sc))
  println(repeat(ca))

}

trait Base {
  type T
  def method: T
}

class Implementation extends Base {
  override type T = Int
  override def method: Int = 43
}