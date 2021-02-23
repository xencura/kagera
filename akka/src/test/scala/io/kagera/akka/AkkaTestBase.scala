package io.kagera.akka

import akka.actor.ActorSystem
import akka.testkit.{ ImplicitSender, TestKit }
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

object AkkaTestBase {

  val defaultTestConfig = ConfigFactory.parseString("""
      |
      |akka {
      |  loggers = ["akka.testkit.TestEventListener"]
      |  persistence {
      |    journal.plugin = "inmemory-journal"
      |    snapshot-store.plugin = "inmemory-snapshot-store"
      |  }
      |}
      |
      |inmemory-read-journal {
      |  write-plugin = "inmemory-journal"
      |  offset-mode = "sequence"
      |  ask-timeout = "10s"
      |  refresh-interval = "50ms"
      |  max-buffer-size = "100"
      |}
      |
      |logging.root.level = WARN
    """.stripMargin)
}

abstract class AkkaTestBase
    extends TestKit(ActorSystem("testSystem", AkkaTestBase.defaultTestConfig))
    with AnyWordSpecLike
    with ImplicitSender
    with BeforeAndAfterAll {

  override def afterAll() = {
    super.afterAll()
    shutdown(system)
  }

  def expectMsgInAnyOrderPF[Out](pfs: PartialFunction[Any, Out]*): Unit = {
    if (pfs.nonEmpty) {
      val total = pfs.reduce((a, b) => a.orElse(b))
      expectMsgPF() {
        case msg @ _ if total.isDefinedAt(msg) =>
          val index = pfs.indexWhere(pf => pf.isDefinedAt(msg))
          val pfn = pfs(index)
          pfn(msg)
          expectMsgInAnyOrderPF[Out](pfs.take(index) ++ pfs.drop(index + 1): _*)
      }
    }
  }
}
