package code

import net.liftweb.common.{Box, Full, Empty, Failure}

object TryUsingBox {
  def main(args: Array[String]): Unit = {
    val fullBox: Box[String] = Full("Hello")
    val emptyBox: Box[String] = Empty
    val failure: Box[String] = Failure("msg", Empty, Empty) // Full(
    //   Failure("another msg", Empty, Empty)
    // ))

    // println(fullBox)
    // println(emptyBox)
    // println(failure)
    //
    // val handleExceptionInBox: Box[String] = net.liftweb.util.Helpers.tryo {
    //   throw new Exception("Help")
    //   "hello?"
    // }
    //
    // println(handleExceptionInBox)

    def mapAndOpen(b: Box[String]): String = {
      b.map(s => s + "!").openOr("nothing here")
    }

    println(mapAndOpen(fullBox))
    println(mapAndOpen(emptyBox))
    println(mapAndOpen(failure))

    println("===============================")

    val full2: Box[String] = Full("hi")
    println(fullBox.or(full2))
    println(emptyBox.or(full2))
    println(emptyBox.or(failure))
    println(failure.or(emptyBox))

    println("===============================")

    println(fullBox ?~ "isFail!")
    println(emptyBox ?~ "isFail!")
    println(failure ?~ "isFail!")
    println(failure ?~! "isFail!") // keep first failure in this chain
  }
}
