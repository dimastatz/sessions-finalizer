import scala.util.Try

val list = List("1", "2")

list
  .find(_ == "3")
  .map(i => i.toInt)
  .map(i => i*2)
  .getOrElse(0)


Try(1/1)
  .map(i => i*2)
  .getOrElse(0)


