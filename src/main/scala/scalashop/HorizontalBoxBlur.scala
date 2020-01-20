package scalashop

import org.scalameter._
import breeze.linalg._

import scala.collection.mutable

object HorizontalBoxBlurRunner {

  val standardConfig = config(
    Key.exec.minWarmupRuns -> 5,
    Key.exec.maxWarmupRuns -> 10,
    Key.exec.benchRuns -> 10,
    Key.verbose -> false
  ) withWarmer (new Warmer.Default)

  def main(args: Array[String]): Unit = {
    val radius = 3
    val width = 1920
    val height = 1080
    val src = new Img(width, height)
    val dst = new Img(width, height)
    //    val seqtime = standardConfig measure {
    //      HorizontalBoxBlur.blur(src, dst, 0, height, radius)
    //    }
    //    println(s"sequential blur time: $seqtime")

    println("With 24 tasks")
    var numTasks = 24
    var partime = standardConfig measure {
      println("Trying again!")
      HorizontalBoxBlur.parBlur(src, dst, numTasks, radius)
    }
    println(s"fork/join blur time with number of tasks = $numTasks: $partime")

    println("With 32 tasks")
    numTasks = 32
    partime = standardConfig measure {
      println("Trying again!")
      HorizontalBoxBlur.parBlur(src, dst, numTasks, radius)
    }
    println(s"fork/join blur time with number of tasks = $numTasks: $partime")

    println("With 40 tasks")
    numTasks = 40
    partime = standardConfig measure {
      println("Trying again!")
      HorizontalBoxBlur.parBlur(src, dst, numTasks, radius)
    }
    println(s"fork/join blur time with number of tasks = $numTasks: $partime")

    //    println(s"speedup: ${seqtime.value / partime.value}")
  }
}

/** A simple, trivially parallelizable computation. */
//noinspection TypeAnnotation
object HorizontalBoxBlur extends HorizontalBoxBlurInterface {

  /** Blurs the rows of the source image `src` into the destination image `dst`,
    * starting with `from` and ending with `end` (non-inclusive).
    *
    * Within each row, `blur` traverses the pixels by going from left to right.
    */
  def blur(src: Img, dst: Img, from: Int, end: Int, radius: Int): Unit = {
    val imageWidth = src.width

    val xCoordinates = 0 until imageWidth
    val yCoordinates = from until end

    for {
      yCoordinate <- yCoordinates
      xCoordinate <- xCoordinates
    } yield dst.update(xCoordinate, yCoordinate, boxBlurKernel(src, xCoordinate, yCoordinate, radius))
  }

  /** Blurs the rows of the source image in parallel using `numTasks` tasks.
    *
    * Parallelization is done by stripping the source image `src` into
    * `numTasks` separate strips, where each strip is composed of some number of
    * rows.
    */
  def parBlur(src: Img, dst: Img, numTasks: Int, radius: Int): Unit = {
    val imageHeight = src.height
    val boundaries = getBoundaries(numTasks, imageHeight)

    import org.json4s._
    import org.json4s.native.Serialization._
    import org.json4s.native.Serialization
    implicit val formats = Serialization.formats(NoTypeHints)

    case class DurationStat(from: Int, to: Int, description: String, duration: Long)

    val allDurations = mutable.Map[(Int, Int, String), Long]()

    boundaries.toList.map {
      case Seq(from: Int, end: Int) =>
        val scheduleTime = System.currentTimeMillis()
        allDurations += (from, end, "ScheduleStart") -> scheduleTime
        val t = task {
          val startTime: Long = System.currentTimeMillis()
          allDurations += (from, end, "TaskStart") -> startTime
          blur(src, dst, from, end, radius)
          val endTime: Long = System.currentTimeMillis()
          allDurations += (from, end, "TaskEnd") -> endTime
          allDurations += (from, end, "TaskDuration") -> (endTime - startTime)
        }
        val endTime = System.currentTimeMillis()
        allDurations += (from, end, "ScheduleEnd") -> endTime
        allDurations += (from, end, "ScheduleDuration") -> (endTime - scheduleTime)
        t
    }.foreach(_.join())

    println {
      write(allDurations.map {
        case ((from, end, description), duration) => DurationStat(from, end, description, duration)
      })
    }

  }

  private def getBoundaries(numTasks: RGBA, imageHeight: RGBA) = {
    linspace(0, imageHeight, numTasks + 1).map(_.toInt).toScalaVector.sliding(2)
  }
}
