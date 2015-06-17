package twitterbot

import rx.lang.scala.{Observable, Subscription}
import twitter4j._
import System.err

class TwitterStatusStream {

  val twitterStream = new TwitterStreamFactory().getInstance()

  val observable: Observable[Status] = Observable.create(observer => {
    println("new subscriber")

    val listener = new StatusListener {
      override def onStallWarning(stallWarning: StallWarning): Unit = ()
      override def onDeletionNotice(statusDeletionNotice: StatusDeletionNotice): Unit = ()
      override def onStatus(status: Status): Unit = observer.onNext(status)
      override def onTrackLimitationNotice(i: Int): Unit = ()
      override def onException(e: Exception): Unit = err.println(e.getMessage)
      override def onScrubGeo(l: Long, l1: Long): Unit = ()
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