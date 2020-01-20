import java.util.concurrent._

import scala.util.DynamicVariable
import org.scalameter._

import scala.collection.mutable

package object scalashop extends BoxBlurKernelInterface {

  /** The value of every pixel is represented as a 32 bit integer. */
  type RGBA = Int

  /** Returns the red component. */
  def red(c: RGBA): Int = (0xff000000 & c) >>> 24

  /** Returns the green component. */
  def green(c: RGBA): Int = (0x00ff0000 & c) >>> 16

  /** Returns the blue component. */
  def blue(c: RGBA): Int = (0x0000ff00 & c) >>> 8

  /** Returns the alpha component. */
  def alpha(c: RGBA): Int = (0x000000ff & c) >>> 0

  /** Used to create an RGBA value from separate components. */
  def rgba(r: Int, g: Int, b: Int, a: Int): RGBA = {
    (r << 24) | (g << 16) | (b << 8) | (a << 0)
  }

  /** Restricts the integer into the specified range. */
  def clamp(v: Int, min: Int, max: Int): Int = {
    if (v < min) min
    else if (v > max) max
    else v
  }

  /** Image is a two-dimensional matrix of pixel values. */
  class Img(val width: Int, val height: Int, private val data: Array[RGBA]) {
    def this(w: Int, h: Int) = this(w, h, new Array(w * h))

    def apply(x: Int, y: Int): RGBA = {
      data(y * width + x)
    }

    def update(x: Int, y: Int, c: RGBA): Unit = data(y * width + x) = c
  }

  /** Computes the blurred RGBA value of a single pixel of the input image. */
  def boxBlurKernel(src: Img, x: Int, y: Int, radius: Int): RGBA = {

    import scalaz._, Scalaz._

    val _clampX = (x: Int) => clamp(x, 0, src.width - 1)
    val _clampY = (y: Int) => clamp(y, 0, src.height - 1)
    val _getRGBA: ((Int, Int)) => RGBA = (src.apply _).tupled

    val deltas = (-radius to radius).toSet

    val pixelCoordinates = for {
      xDelta <- deltas
      yDelta <- deltas
    } yield (_clampX(xDelta + x), _clampY(yDelta + y))

    val `all rgba's` = pixelCoordinates.map(_getRGBA).map(rgba => (red(rgba), green(rgba), blue(rgba), alpha(rgba)))
    val normalize = (color: Int) => color / `all rgba's`.size
    val sums = `all rgba's`.reduce((x, y) => x |+| y)

    rgba(normalize(sums._1), normalize(sums._2), normalize(sums._3), normalize(sums._4))
  }

  val forkJoinPool = new ForkJoinPool

  abstract class TaskScheduler {
    def schedule[T](body: => T): ForkJoinTask[T]

    def parallel[A, B](taskA: => A, taskB: => B): (A, B) = {
      val right = task {
        taskB
      }
      val left = taskA
      (left, right.join())
    }
  }

  class DefaultTaskScheduler extends TaskScheduler {
    def schedule[T](body: => T): ForkJoinTask[T] = {
      val t = new RecursiveTask[T] {
        def compute = body
      }
      Thread.currentThread match {
        case wt: ForkJoinWorkerThread =>
          t.fork()
        case _ =>
          forkJoinPool.execute(t)
      }
      t
    }
  }

  val scheduler =
    new DynamicVariable[TaskScheduler](new DefaultTaskScheduler)

  def instrumentedTask[TaskId,T](logger: mutable.Map[(TaskId, String), Long])(id: TaskId)(body: => T): ForkJoinTask[T] = {
    val scheduleStart = System.currentTimeMillis()
    logger += (id, "ScheduleStart") -> scheduleStart
    val t = task {
      val taskStart = System.currentTimeMillis()
      logger += (id, "TaskStart") -> taskStart
      val _t = body
      val taskEnd = System.currentTimeMillis()
      logger += (id, "TaskEnd") -> taskEnd
      logger += (id, "TaskDuration") -> (taskEnd - taskStart)
      _t
    }
    val scheduleEnd = System.currentTimeMillis()
    logger += (id, "ScheduleEnd") -> scheduleEnd
    logger += (id, "ScheduleDuration") -> (scheduleEnd - scheduleStart)
    t
  }

  def task[T](body: => T): ForkJoinTask[T] = {
    scheduler.value.schedule(body)
  }

  def parallel[A, B](taskA: => A, taskB: => B): (A, B) = {
    scheduler.value.parallel(taskA, taskB)
  }

  def parallel[A, B, C, D](taskA: => A, taskB: => B, taskC: => C, taskD: => D): (A, B, C, D) = {
    val ta = task {
      taskA
    }
    val tb = task {
      taskB
    }
    val tc = task {
      taskC
    }
    val td = taskD
    (ta.join(), tb.join(), tc.join(), td)
  }

  // Workaround Dotty's handling of the existential type KeyValue
  implicit def keyValueCoerce[T](kv: (Key[T], T)): KeyValue = {
    kv.asInstanceOf[KeyValue]
  }
}
