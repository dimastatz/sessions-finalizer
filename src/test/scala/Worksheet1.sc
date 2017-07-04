import java.time._
import java.time.format.DateTimeFormatter

import com.google.gson.{Gson, GsonBuilder}
import scala.util.Random


val serializer: Gson = new GsonBuilder().create()

case class A1(i: Int, t: LocalDateTime)

val x = A1(1, LocalDateTime.now(ZoneId.of("UTC")))
val xString = serializer.toJson(x)





val y = serializer.fromJson(xString, classOf[A1])
