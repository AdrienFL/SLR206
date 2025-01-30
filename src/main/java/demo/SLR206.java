package demo;

import java.util.ArrayList;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class SLR206 {
	
	public static void main(String[] args) {
		int N = 10;


		ArrayList<ActorRef> actorList = new ArrayList<ActorRef>();
		

		final ActorSystem system = ActorSystem.create("system");
		final LoggingAdapter log = Logging.getLogger(system, "main");


		for (int i = 0; i< N; i++){
			actorList.add(system.actorOf(FirstActor.createActor(), Integer.toString(i)));
		}


	    for(int x= 0; x <10; x++){
			for(int y = 0; y < 10; y++){
				actorList.get(x).tell(actorList.get(y), ActorRef.noSender());
			}
		}
		

 
	    try {
			waitBeforeTerminate();
		} catch (InterruptedException ex) {
			ex.printStackTrace();
		} finally {
			system.terminate();
		}
	}
	public static void waitBeforeTerminate() throws InterruptedException {
		Thread.sleep(5000);
	}

}
