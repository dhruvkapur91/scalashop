package scalashop

import org.scalameter._
import scala.concurrent._
import duration._


object IteratorVsListInFutures extends App {
  def slowFunction = Thread.sleep(1000)

  import scala.concurrent.ExecutionContext.Implicits.global

  println("Should take approximately 4000 ms or 4 sec")
  println{
    withWarmer(new Warmer.Default) measure {
      List(1,2,3,4).foreach(_ => slowFunction)
    }
  }

  println("Should take approximately 1 second")
  println {
    withWarmer(new Warmer.Default) measure {
      val futures: Seq[Future[Unit]] = List(1,2,3,4).map(_ => Future { slowFunction})
      futures.foreach(x => Await.result(x, 10.seconds))
    }
  }

  println("And how long does this take")
  println {
    withWarmer(new Warmer.Default) measure {
      val futures = List(1,2,3,4).iterator.map(_ => Future { slowFunction})
      futures.foreach(x => Await.result(x, 10.seconds))
    }
  }

}

object TryThisFutureTraverseThing extends App {
  import scala.concurrent.ExecutionContext.Implicits.global

}