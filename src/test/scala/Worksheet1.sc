import com.typesafe.config.ConfigFactory

val a = ConfigFactory.load("C:\\Git\\Pipeline\\sessions-finalizer\\app.conf")
a.getString("conf.host")
