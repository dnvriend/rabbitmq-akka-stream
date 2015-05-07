package io.scalac.rabbit

import scala.concurrent.Future
import scala.util.{Try, Failure, Success}

import akka.actor.ActorSystem
import akka.util.ByteString

import akka.stream.scaladsl._

import com.typesafe.scalalogging.slf4j.LazyLogging

import akka.stream.ActorFlowMaterializer

import io.scalac.amqp.{Connection, Message, Queue}

import io.scalac.rabbit.RabbitRegistry._

class ConsumerBootable extends akka.kernel.Bootable {
  override def startup(): Unit = {
    ConsumerApp.main(Array.empty[String])
  }

  override def shutdown(): Unit = {

  }
}

object ConsumerApp extends App with FlowFactory with LazyLogging {

  implicit val actorSystem = ActorSystem("rabbit-akka-stream")
  
  import actorSystem.dispatcher
  
  implicit val materializer = ActorFlowMaterializer()

  def retry[T](num: Int, retried: Int = 0)(block: => T): Try[T] = Try(block) match {
    case r if r.isSuccess => r
    case r if r.isFailure && num >= retried =>
      Thread.sleep(1000)
      retry(num, retried + 1)(block)
    case u => Failure(new RuntimeException("Retries exceeded"))
  }

  val connection: Connection =
    retry[Connection](10) { Connection() }
      .getOrElse {
        println("Could not make a connection with RabbitMQ")
        System.exit(1)
        null
     }

  
  setupRabbit() onComplete { 
    case Success(_) =>
      logger.info("Exchanges, queues and bindings declared successfully.")
    
      val rabbitConsumer = Source(connection.consume(inboundQueue.name))
      val rabbitPublisher = Sink(connection.publish(outboundExchange.name))
      
      val flow = rabbitConsumer via consumerMapping via domainProcessing via publisherMapping to rabbitPublisher
    
      logger.info("Starting the flow")
      flow.run()
      
      logger.info("Starting the trial run")
      trialRun()
    case Failure(ex) =>
      logger.error("Failed to declare RabbitMQ infrastructure.", ex)
  }  
  
  def setupRabbit(): Future[List[Queue.BindOk]] =
    Future.sequence(List(
        
      /* declare and bind inbound exchange and queue */
      Future.sequence {
        connection.exchangeDeclare(inboundExchange) :: 
        connection.queueDeclare(inboundQueue) :: Nil
      } flatMap { _ =>
        Future.sequence {
	      connection.queueBind(inboundQueue.name, inboundExchange.name, "") :: Nil
        }
      },

      /* declare and bind outbound exchange and queues */
      Future.sequence {
        connection.exchangeDeclare(outboundExchange) :: 
        connection.queueDeclare(outOkQueue) ::
        connection.queueDeclare(outNokQueue) :: Nil
      } flatMap { _ =>
        Future.sequence {
          connection.queueBind(outOkQueue.name, outboundExchange.name, outOkQueue.name) ::
	      connection.queueBind(outNokQueue.name, outboundExchange.name, outNokQueue.name) :: Nil
        }
      }
    )).map { _.flatten }
  
  /**
   * Trial run of couple of messages just to show that streaming through RabbitMQ actually works here.
   * 
   * We're setting up two streams here:
   * 1. Stream publishing trial messages to RabbitMQ inbound exchange.
   * 2. Stream consuming the trial messages from one of the outbound queues.
   * 
   * Both streams are set up to die after performing their purpose.
   */
  def trialRun() = {
    val trialMessages = "message 1" :: "message 2" :: "message 3" :: "message 4" :: "message 5" :: Nil
    
    /* publish couple of trial messages to the inbound exchange */
    Source(trialMessages).
      map(msg => Message(ByteString(msg))).
      runWith(Sink(connection.publish(inboundExchange.name, "")))
      
    /* log the trial messages consumed from the queue */
    Source(connection.consume(outOkQueue.name)).
      take(trialMessages.size).
      map(msg => logger.info(s"'${msg.message.body.utf8String}' delivered to ${outOkQueue.name}")).
      runWith(Sink.onComplete({
        case Success(_) => logger.info(
          """
            |Trial run finished. You can now go to http://boot2docker:8080/ (guest:guest) and try publishing messages manually.
            |
            |Find an exchange named 'censorship.inbound.exchange' and start publishing messages.
            |
            |You should observe two effects:
            | 1. Text of your message will be logged to a console,
            | 2. Modified version of your message will land in either 'censorship.ok.queue' or 'censorship.nok.queue' (based on the text content of your message).
            |
            | You will notice that messages containing the word 'terror' will go to the 'nok' queue.
            | This is the initial censorship filter.
            | Feel free to modify the "forbidden words" list.
            |""".stripMargin)
        case Failure(ex) => logger.error("Trial run finished with error.", ex)}))
  }
}