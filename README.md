rabbitmq-akka-streams
=====================
This project has been created with Typesafe Activator to play around with Akka Streams and RabbitMQ. It is for study
purposes only. The project has been made compatible with the API of akka-streams `v1.0-RC2` and uses the latest 
reactive-rabbit `v1.0.0`, which is the `Reactive Streams driver for AMQP protocol` for RabbitMQ.

# Usage
Launch the project with the `test.sh` script that will build the project and package it using Docker. Then RabbitMQ
and the Activator project will be launched.

The application will first do a trial run of messages. Using the source code one can deduce how the flow works. 

After the trial run has finished, go to [http://boot2docker:8080](http://boot2docker:8080) and login with the credentials
(guest:guest) on the RabbitMQ management console and try publishing messages manually.

## Publishing messages manually
Find an exchange named `censorship.inbound.exchange` and start publishing messages. You should do this by clicking on 
the `queues` tab, then clicking on the `Queue censorship.inbound.queue` text and then scroll down and open the 
`publish message` drawer. In the user interface you can type text in the `Payload` text field and click on the button
`publish message`.

You should observe two effects:

1. Text of your message will be logged to a console,
2. Modified version of your message will land in either `censorship.ok.queue` or `censorship.nok.queue` (based on the text content of your message).

You will notice that messages containing the word `terror` will go to the `censorship.nok.queue` queue. This is the initial censorship filter.

Feel free to modify the "forbidden words" list in the object `DomainService`.
 
 