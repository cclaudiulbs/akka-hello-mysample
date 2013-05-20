package com.cc.akka.akkalearning.firstattempt;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;

/**
 * When using an Akka Concurrency System first think of the Type of Messages you have in the System.
 * There can be as many messages as you want; We declare them as Strongly Typed as static classes, instead using the nasty String-like format.
 * This small Actor-based-demo app, uses three Actors: 
 * <ul>MasterHelloActor</ul>
 * <ul>PostmanActor</ul>
 * <ul>BuilderHelloActor</ul>
 * 
 * The message processing flow is:
 * 1. The MasterActor initiates the system, by telling/sending a message to the Postman that it needs a message build back
 * 2. The PostmanActor is "checking" the mailbox, and onReceive() handler, handles a request message, which then tells/informs the
 * 3. The BuilderActor to build the message, wrapped inside a HelloMessage Object, which then returns hands-it-over to the Master
 * 4. The MasterActor displays it and closes the system, being the last actor that processed the message, and hence "cleans" all
 * the Actors underneath him.
 * 
 * @author cclaudiu
 */
public class HelloMessageDemo {
	
	// --------------------- Messages -------------------------//
	static class HelloMessage {
		private final String someMessage;
		public HelloMessage(String someMessage) {
			this.someMessage = someMessage;
		}
		public String getSomeMessage() {
			return someMessage;
		}
	}
	
	static class BuildRequestMessage {
	}
	
	enum InitComputationMessage {
		INIT;
	}

	// ------------------ Actors --------------------------------//
	static class MasterHelloActor extends UntypedActor {
		@Override
		public void onReceive(Object message) throws Exception {
			
			if(message instanceof InitComputationMessage) {
				ActorRef postmanActor = getContext().system().actorOf(new Props(PostmanActor.class));
				// inform the Postman of this init request message, to "talk" to the Builder and build the message
				postmanActor.tell(new BuildRequestMessage(), getSelf());
				
			} else if(message instanceof HelloMessage) {
				HelloMessage decodedMessage = (HelloMessage) message;
				System.out.println("message build by Builder Actor, and retrieved from the PostmanActor, is displayed by the Master Actor: " 
										+ decodedMessage.getSomeMessage());
				
				// Last actor in-charge, that is the last actor which "has-something-to-say" will close the System
				getContext().system().shutdown();
			}
		}
	}
	
	static class BuilderHelloActor extends UntypedActor {
		@Override
		public void onReceive(Object rawMessage) throws Exception {
			
			if(rawMessage instanceof BuildRequestMessage) {
				// build the hello-message, by wrapping it inside a HelloMessage Object and inform/tell the sender
				getSender().tell(new HelloMessage("Hello There! was build by the Builder Actor"), getSelf());
			}
		}
	}
	
	static class PostmanActor extends UntypedActor {
		@Override
		public void onReceive(Object message) throws Exception {
			
			if(message instanceof BuildRequestMessage) {
				ActorRef builderActor = getContext().system().actorOf(new Props(BuilderHelloActor.class));
				builderActor.tell(new BuildRequestMessage(), getSelf());
				
			} else if(message instanceof HelloMessage) {
				ActorRef masterActorRef = getContext().system().actorOf(new Props(MasterHelloActor.class));
				masterActorRef.tell(message, getSelf());
			}
		}
		
	}
	
	// -------------- Client ---------------------//
	public static void main(String[] args) {
		ActorSystem akkaSystem = ActorSystem.create("HelloMessageSystem");
		ActorRef masterActor = akkaSystem.actorOf(new Props(MasterHelloActor.class), "masterActor");
		
//		 directly without the Builder Actor && the Postman Actor
//		masterActor.tell(new HelloMessage("Heeeeloo there!"), masterActor);
		
		
		// through the BuilderActor && Postman Actor
		masterActor.tell(InitComputationMessage.INIT, masterActor);
	}
}