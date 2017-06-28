trait A {
  def a(): Int
  def b(): Int = 2

}


val x: A = () => 1

x.a()
x.b()


