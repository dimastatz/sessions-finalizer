trait A {
  def a(i: Int): Int
  def b(): Int = 2

}


val x: A = i => i*2

x.a(2)
x.b()


