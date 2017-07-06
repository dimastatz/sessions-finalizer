import com.google.gson.{Gson, GsonBuilder}


case class A(a: Int, b: String)

val serializer: Gson = new GsonBuilder().create()
val x = A(1, "a")
val s = serializer.toJson(x)
val y: A = serializer.fromJson(s, classOf[A])
