/*
 * Copyright 2017-2019 John A. De Goes and the ZIO Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package scalaz.zio

import scalaz.zio.internal.{ FiberContext, Platform }

/**
 * A `Runtime[R]` is capable of executing tasks within an environment `R`.
 */
trait Runtime[+R] {

  /**
   * The environment of the runtime.
   */
  val Environment: R

  val Platform: Platform

  /**
   * Provided the environment for the specified task, including a
   * [[scalaz.zio.platform.Platform]], executes the task synchronously, failing
   * with [[scalaz.zio.FiberFailure]] if there are any errors. May fail on
   * Scala.js if the task cannot be entirely run synchronously.
   *
   * This method is effectful and should only be done at the edges of your program.
   */
  final def unsafeRun[E, A](zio: ZIO[R, E, A]): A =
    unsafeRunSync(zio).getOrElse(c => throw new FiberFailure(c))

  /**
   * Provided the environment for the specified task, including a
   * [[scalaz.zio.platform.Platform]], executes the task synchronously. May
   * fail on Scala.js if the task cannot be entirely run synchronously.
   *
   * This method is effectful and should only be done at the edges of your program.
   */
  final def unsafeRunSync[E, A](zio: ZIO[R, E, A]): Exit[E, A] = {
    val result = internal.OneShot.make[Exit[E, A]]

    unsafeRunAsync(zio)((x: Exit[E, A]) => result.set(x))

    result.get()
  }

  /**
   * Provided the environment for the specified task, including a
   * [[scalaz.zio.platform.Platform]], executes the task asynchronously,
   * eventually passing the exit value to the specified callback.
   *
   * This method is effectful and should only be done at the edges of your program.
   */
  final def unsafeRunAsync[E, A](zio: ZIO[R, E, A])(k: Exit[E, A] => Unit): Unit = {
    val context = new FiberContext[E, A](Platform)

    context.evaluateNow(zio.provide(Environment))
    context.runAsync(k)
  }

  /**
   * Provided the environment for the specified task, including a
   * [[scalaz.zio.platform.Platform]], executes the task asynchronously,
   * discarding the result of execution.
   *
   * This method is effectful and should only be done at the edges of your program.
   */
  final def unsafeRunAsync_[E, A](zio: ZIO[R, E, A]): Unit =
    unsafeRunAsync(zio)(_ => ())
}
object Runtime {

  /**
   * Builds a new runtime given an environment `R` and a [[scalaz.zio.Platform]].
   */
  final def apply[R](r: R, platform: Platform): Runtime[R] = new Runtime[R] {
    val Environment = r
    val Platform    = platform
  }
}
