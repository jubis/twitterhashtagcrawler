import com.twitter.util.{Await, Future}
import io.finch.response._
import io.finch.route._
import io.finch._
import com.twitter.finagle.{Service, Httpx}

object TwitterBot {

  val endpoint = Get /> hello

  def hello() = new Service[HttpRequest, HttpResponse] {
    def apply(req: HttpRequest) = Future(Ok("Hello! I'm TwitterBot"))
  }

	def main(args: Array[String]) {
    val _ = Await.ready(Httpx.serve(":8080", endpoint.toService))
	}
}
