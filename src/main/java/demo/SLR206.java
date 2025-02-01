package demo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class SLR206 {
		public static int N = 3;
		public static int f = 0;

	public static void main(String[] args) {
		// private long start_time, end_time;

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
		// start_time = System.currentTimeMillis();
		for(int i =0; i<N; i++){
			if(i<f){
				actorList.get(i).tell(crashMessage, ActorRef.noSender());
			}
			else{

				actorList.get(i).tell(launchMessage, ActorRef.noSender());
			}
		}

	    try {
			waitBeforeTerminate();
		} catch (InterruptedException ex) {
			ex.printStackTrace();
		} finally {
			compute_stats(actorList, f, N);
			system.terminate();
		}
	}
	public static void waitBeforeTerminate() throws InterruptedException {
		while (true) {
			Thread.sleep(1000);
			if (Process.getProcessList().stream().allMatch(Process::isDone)) {
				break;
			}
		}
	}

	public static void compute_stats(ArrayList<ActorRef> actorList, int f, int N){
		long total_write_waiting = 0;
		long total_read_waiting = 0;
		long total_time_running = 0;


		for(Process p: Process.getProcessList()){
			total_write_waiting += p.getWriteWaitingTime();
			total_read_waiting += p.getReadWaitingTime();
			total_time_running += p.getRunningTime();
		}

		File file = new File("stats.txt");
		FileWriter fw = null;
		try {
			if (!file.exists()) {
				file.createNewFile();
			}
			fw = new FileWriter(file.getAbsoluteFile());

			fw.write("Run N = "+N+" f = "+f+"\n");
			fw.write("Total write waiting time = "+total_write_waiting/1000+" s \n");
			fw.write("Total read waiting time = "+total_read_waiting/1000+" s \n");
			fw.write("Total time running = "+total_time_running/1000+" s\n");

			fw.write("Average write waiting time per process = "+(total_write_waiting/N)+" ms\n");
			fw.write("Average read waiting time per process = "+(total_read_waiting/N)+" ms\n");
			fw.write("Average time running per process = "+(total_time_running/N)+" ms\n");

			fw.write("Average write waiting time per operation = "+(total_write_waiting/(N*Process.M))+" ms\n");
			fw.write("Average read waiting time per operation = "+(total_read_waiting/(N*Process.M))+" ms\n");
			fw.write("Average time running per operation = "+(total_time_running/(N*Process.M))+" ms\n");

			fw.write("Throughput = "+((N*Process.M)/(total_time_running/1000.0))+" op/s\n");

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (fw != null) {
				try {
					fw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

}
