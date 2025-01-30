package demo;

import java.util.ArrayList;
import java.util.Collections;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class SLR206 {
	
	public static void main(String[] args) {
		int N = 3;
		int f = 0;

		ArrayList<ActorRef> actorList = new ArrayList<>();
		

		final ActorSystem system = ActorSystem.create("system");
		final LoggingAdapter log = Logging.getLogger(system, "main");


		for (int i = 0; i< N; i++){
			ActorRef a = system.actorOf(Process.createActor(), Integer.toString(i));
			actorList.add(a);
		}


	    for(int x= 0; x <N; x++){
			for(int y = 0; y < N; y++){
				
				actorList.get(x).tell(actorList.get(y), ActorRef.noSender());
				
			}
		}
		Collections.shuffle(actorList);

		MyMessage crashMessage = new MyMessage("crash", "0");
		MyMessage launchMessage = new MyMessage("launch", "0");
		for(int i =0; i<N; i++){
			if(i<f){
				//actorList.get(i).tell(crashMessage, ActorRef.noSender());
			}
			else{

				//actorList.get(i).tell(launchMessage, ActorRef.noSender());
			}
		}
		actorList.get(0).tell(launchMessage, ActorRef.noSender());
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
