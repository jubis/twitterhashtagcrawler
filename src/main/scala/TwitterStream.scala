import rx.Scheduler
import rx.lang.scala.{Scheduler, Observable, Subscription}
import rx.schedulers.Schedulers
import twitter4j._
import twitter4j.conf.ConfigurationBuilder

class TwitterStatusStream {

  val twitterStream = new TwitterStreamFactory().getInstance()


  val observable: Observable[Status] = Observable.create(observer => {
    val start = System.currentTimeMillis()

    val listener = new StatusListener {

      override def onStallWarning(stallWarning: StallWarning): Unit = println("Stall")

      override def onDeletionNotice(statusDeletionNotice: StatusDeletionNotice): Unit = ()

      override def onStatus(status: Status): Unit = observer.onNext(status)
      override def onTrackLimitationNotice(i: Int): Unit = println("track limit")

      override def onException(e: Exception): Unit = println("Exception")

      override def onScrubGeo(l: Long, l1: Long): Unit = println("Geo")
    }

    twitterStream.addListener(listener)
    Subscription(twitterStream.removeListener(listener))
  })

  def stream(): Observable[Status] = {
    observable
  }

  def sample(tags : Array[String]) {
    twitterStream.filter(new FilterQuery().track(tags))
  }
}

object TwitterStatusStream {
  def apply() = new TwitterStatusStream()
}