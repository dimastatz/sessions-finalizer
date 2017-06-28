trait A {
  def a(): Int
  def b(): Int = 2
  def c(): Int
}


val x = new {} with A {
  def a() = 1
  def c() = 3
}

x.a()
x.b()
x.c()


