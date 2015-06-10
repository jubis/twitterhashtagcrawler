import com.twitter.util.{Await, Future}
import io.finch.response._
import io.finch.route._
import io.finch._
import com.twitter.finagle.{Service, Httpx}

import scala.util.Properties

object TwitterBot {

  val endpoint = Get /> hello

  def hello() = new Service[HttpRequest, HttpResponse] {
    def apply(req: HttpRequest) = Future(Ok("Hello! I'm HashTagCrawler"))
  }

	def main(args: Array[String]) {
    val port = Properties.envOrElse("PORT", "8080")
    val _ = Await.ready(Httpx.serve(s":$port", endpoint.toService))
	}
}
