import java.time._
import java.time.format.DateTimeFormatter

import scala.util.Random


private val rand = new Random(LocalTime.now().getSecond)
private val epoch = ZonedDateTime.of(2015, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"))

val duration = Duration.between(epoch, ZonedDateTime.now(ZoneId.of("UTC")))
val milliseconds: Long = duration.getSeconds * 1000
val sid: Long =  (milliseconds << 14) + rand.nextInt(1000)
sid