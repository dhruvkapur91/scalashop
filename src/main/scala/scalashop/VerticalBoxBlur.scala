package scalashop

import org.scalameter._

object VerticalBoxBlurRunner {

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

    val seqtime = standardConfig measure {
      VerticalBoxBlur.blur(src, dst, 0, width, radius)
    }
    println(s"sequential blur time: $seqtime")

    val numTasks = 32
    val partime = standardConfig measure {
      VerticalBoxBlur.parBlur(src, dst, numTasks, radius)
    }
    println(s"fork/join blur time: $partime")
    println(s"speedup: ${seqtime.value / partime.value}")
  }

}

/** A simple, trivially parallelizable computation. */
object VerticalBoxBlur extends VerticalBoxBlurInterface {

  /** Blurs the columns of the source image `src` into the destination image
    * `dst`, starting with `from` and ending with `end` (non-inclusive).
    *
    * Within each column, `blur` traverses the pixels by going from top to
    * bottom.
    */
  def blur(src: Img, dst: Img, from: Int, end: Int, radius: Int): Unit = {

    val imageHeight = src.height

    val xCoordinates: Seq[Int] = from until end
    val yCoordinates: Seq[Int] = 0 until imageHeight

    for {
      xCoordinate <- xCoordinates
      yCoordinate <- yCoordinates
    } yield dst.update(xCoordinate, yCoordinate, boxBlurKernel(src, xCoordinate, yCoordinate, radius))

  }

  def parBlur(src: Img, dst: Img, numTasks: Int, radius: Int): Unit = {
    val imageWidth = src.width
    val stepSize = Math.max(imageWidth / numTasks, 1)
    val boundaries: Iterator[IndexedSeq[RGBA]] = (0 to imageWidth by stepSize).sliding(2)
    val tasks = boundaries.toList.map { case Seq(from, end) => task {
      blur(src, dst, from, end, radius)
    }
    }
    tasks.foreach(_.join())
  }

}