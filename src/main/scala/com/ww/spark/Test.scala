package com.ww.spark

object Test {
  def main(args: Array[String]): Unit = {
    val thingNotSerializable = new SomeThingNotSerializable
    //    val method = thingNotSerializable.someMethod
    //    println(method)
    thingNotSerializable.scope("aaa")(println(111))

//    val reduce = Iterator[Int] => Option[Int] = iter => {
//      None
//    }
  }
}

object Obj {
  val s1 = <a/><b/>

  def main(args: Array[String]): Unit = {
    println("hi")
  }
}

class SomeThingNotSerializable {
  def scope(name: String)(body: => Unit) = body

  def someValue = 1

  def someMethod() = scope("one") {
    def y = someValue

    scope("two") {
      println(y + 1)
    }
  }

  def main(args: Array[String]): Unit = {
    val test = someMethod()
    println(test)
  }
}

object SomeThingNotSerializable