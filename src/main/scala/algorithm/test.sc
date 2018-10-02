val arr = List(1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1)
val env = Map(0 -> 'a', 1 ->'b',2 -> 'c', 3 ->'d',4 -> 'e')
arr.grouped(5).map(group => {
  val countsOnes = group.count(_ == 1)
  if( countsOnes == 1 ) {
    env(group.indexOf(1))
  } else 'x'
}).toList

/*val matrix = Array(
  Array(0, 0, 0, 1, 0, 1),
  Array(0, 0, 0, 0, 0, 0),
  Array(0, 0, 0, 0, 0, 0),
  Array(0, 1, 0, 0, 0, 0),
  Array(0, 0, 0, 0, 0, 0),
  Array(0, 0, 1, 0, 0, 0),
  Array(0, 0, 0, 0, 0, 0),
  Array(1, 0, 0, 0, 0, 0),
  Array(0, 0, 0, 0, 1, 0),
  Array(0, 0, 0, 0, 0, 0)
)
def getCodeofMatrix(matrix: Array[Array[Int]]) = {
  val code = Array.fill(6)('_')
  matrix.foldLeft((code,0)){(co,arr) =>
    val indexs = arr.zipWithIndex.filter(_._1 == 1).map(_._2)
    indexs.foreach(c => co._1.update(c,co._2.toString.charAt(0)))
    (co._1,co._2 + 1)
  }._1.mkString("")
}
getCodeofMatrix(matrix)*/
/*
val points = Array((1319.0, 164.0), (238.0, 166.0), (196.0, 1222.0), (1393.0, 1200.0))


def sorted(points:Array[(Double, Double)],refer:(Double, Double)): Array[(Double,Double)] = {
  def distance(p1:(Double, Double), p2:(Double,Double)): Double = {
    val d1 = math.pow(p1._1 - p2._1,2)
    val d2 = math.pow(p1._2 - p2._2,2)
    math.sqrt(d1 + d2)
  }
  val arr = points.map((point) => {
    (distance(refer,point),point)
  }).sortWith(_._1 < _._1)

  val x1_y1 = arr.head._2
  val x3_y3 = arr.last._2
  val subArr = Array(arr(1),arr(2)).sortWith(_._2._1 < _._2._1)
  val x4_y4 = subArr.head._2
  val x2_x2 = subArr.last._2
  Array(x1_y1,x2_x2,x3_y3,x4_y4)
}

sorted(points,(0.0,0.0))
*/



/*import scala.util.control.Breaks.{breakable, break}
breakable {
  for (i <- 1 to 9) {
    println(i)
    if (i > 4) break  // break out of the for loop
  }
}*/

/*def memoizedIsPrime: Int => Boolean = {
  def isPrime(num: Int): Boolean = {
    2 to (num-1) forall(x => num % x != 0)
  }

  val cache = collection.mutable.Map.empty[Int, Boolean]

  num => cache.getOrElse(num, {
    print(s"\n Calling isPrime since input ${num} hasn't seen before and caching the output")
    cache.update(num, isPrime(num))
    cache(num)
  })
}

val isPrime = memoizedIsPrime
isPrime(10)
isPrime(7)
isPrime(10)*/
/*def foobat(x: Int)(y: Int) = x + y
val f1 = foobat(10)_
f1(20)

case class Person(name: String, age: Int)
val o1 = Some(40)
val o2 = Some("Ernesto")
val o3 = None
for {
  n <- o3
  a <- o1
} yield Person(n,a)*/

/*val arr =  Array(1,2,3,5,3,6,3,5)
arr.filter(_ == 3)
arr.grouped(3).map(_.mkString(" ")).toArray
arr.groupBy(identity).mapValues(_.size  )*/
/*val arr = "3 2 1 3".split(" ").map(_.trim.toInt)
def birthdayCakeCandles(ar: Array[Int]): Int = {
  val map = ar.groupBy(identity).mapValues(_.length)
  val max = ar.max
  map(max)
}

birthdayCakeCandles(arr)*/
/*

val str = "ibfdgaeadiaefgbhbdghhhbgdfgeiccbiehhfcggchgghadhdhagfbahhddgghbdehidbibaeaagaeeigffcebfbaieggabcfbiiedcabfihchdfabifahcbhagccbdfifhghcadfiadeeaheeddddiecaicbgigccageicehfdhdgafaddhffadigfhhcaedcedecafeacbdacgfgfeeibgaiffdehigebhhehiaahfidibccdcdagifgaihacihadecgifihbebffebdfbchbgigeccahgihbcbcaggebaaafgfedbfgagfediddghdgbgehhhifhgcedechahidcbchebheihaadbbbiaiccededchdagfhccfdefigfibifabeiaccghcegfbcghaefifbachebaacbhbfgfddeceababbacgffbagidebeadfihaefefegbghgddbbgddeehgfbhafbccidebgehifafgbghafacgfdccgifdcbbbidfifhdaibgigebigaedeaaiadegfefbhacgddhchgcbgcaeaieiegiffchbgbebgbehbbfcebciiagacaiechdigbgbghefcahgbhfibhedaeeiffebdiabcifgccdefabccdghehfibfiifdaicfedagahhdcbhbicdgibgcedieihcichadgchgbdcdagaihebbabhibcihicadgadfcihdheefbhffiageddhgahaidfdhhdbgciiaciegchiiebfbcbhaeagccfhbfhaddagnfieihghfbaggiffbbfbecgaiiidccdceadbbdfgigibgcgchafccdchgifdeieicbaididhfcfdedbhaadedfageigfdehgcdaecaebebebfcieaecfagfdieaefdiedbcadchabhebgehiidfcgahcdhcdhgchhiiheffiifeegcfdgbdeffhgeghdfhbfbifgidcafbfcd"
isValid(str)
def isValid(s: String) = {
  val ss = s.mkString(" ").split(" ")
  val map = ss.groupBy(identity).mapValues(_.size)
  val list = map.groupBy(_._2).map { case v => (v._1, v._2.toList.map(_._1))}.toList
  val difference = list.map(_._1).max - list.map(_._1).min
  println(list)
  if(list.length == 2) {
    if(list.exists(v => v._1 == 1 && v._2.length > 1)) "NO"
    else if(difference != 1 && !list.exists(v => v._1 == 1 && v._2.length == 1) ) "NO"
    else "YES"
  }
  else if(list.length == 1 ) "YES"
  else "NO"
}
*/

/*
val s = "12:05:45AM"
def timeConversion(s: String): String = {
  val _PM = "PM"
  val arr = s.split(":")
  val time = arr(2).substring(2)
  val oldHour = arr(0).toInt
  if (time == _PM) {
    if(oldHour == 12) {
      arr(0) + ":" + arr(1) + ":" +arr(2).substring(0,2)
    }
    else {
      (arr(0).toInt + 12).toString + ":" + arr(1) + ":" + arr(2).substring(0,2)
    }

  } else {
    if(oldHour == 12) {
       "00:" + arr(1) + ":" +arr(2).substring(0,2)
    }
    else {
      arr(0) + ":" + arr(1) + ":" +arr(2).substring(0,2)
    }
  }
}
timeConversion(s)*/
