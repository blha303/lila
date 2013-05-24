package lila.hub

import scala.concurrent.duration._

import akka.actor._
import akka.pattern.ask

import actorApi.Tell
import makeTimeout.short

final class ActorMap[A <: Actor](mkActor: String ⇒ A) extends Actor {

  private case class Get(id: String)

  def receive = {

    case Get(id) ⇒ sender ! {
      (actors get id) | {
        context.actorOf(Props(mkActor(id)), name = id) ~ { actor ⇒
          actors = actors + (id -> actor)
          context watch actor
        }
      }
    }

    case Tell(id, msg)   ⇒ get(id) foreach { _ forward msg }

    case Terminated(actor) ⇒ actors find (_._2 == actor) foreach {
      case (id, _) ⇒ actors = actors - id
    }
  }

  private def get(id: String): Fu[ActorRef] = self ? Get(id) mapTo manifest[ActorRef]

  private var actors = Map[String, ActorRef]()
}
