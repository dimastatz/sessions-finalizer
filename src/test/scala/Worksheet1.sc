
trait A {
  def x(): Int = 1
  def y(): Int
}


class B extends A {
  def y() = 2
}


val b = new B
b.x()
b.y()


