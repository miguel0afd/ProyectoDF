import akka.actor.Actor
import akka.actor.ActorPath
import akka.cluster.client.{ClusterClientSettings, ClusterClient}
import akka.pattern.Patterns
import proyectoDF.cluster.mensajeria.{peticionDF, respuestaDF}
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}


class ActorClienteDataFederation extends Actor {

  val initialContacts = Set(
    ActorPath.fromString("akka.tcp://ClusterDataFederation@192.168.1.110:2551/system/receptionist"))
  val settings = ClusterClientSettings(context.system)
    .withInitialContacts(initialContacts)

  val c = context.system.actorOf(ClusterClient.props(settings), "ClienteDataFederation")


  def receive = {
    case respuestaDF(result) => {
      println("Client response")
      println(result)
    }
    case EnviarPeticion(peticion) => {
      val job = peticionDF("Peticion - " + peticion)
      implicit val timeout = Timeout(5 seconds)
      val result = Patterns.ask(c, ClusterClient.Send("/user/nodo", job, localAffinity = true), timeout)

      result.onComplete {
        case Success(transformationResult) => {
          println(s"Client saw result: $transformationResult")
          self ! transformationResult
        }
        case Failure(t) => println("An error has occured: " + t.getMessage)
      }
    }
  }
}
