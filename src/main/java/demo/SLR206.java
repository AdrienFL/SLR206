package demo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;
import java.util.SortedSet;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class SLR206 {
		public static int N = 100;
		public static int f = 49;
		public static int M = 100;

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
		log.info("wait for ref to propagate");
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			log.error(e, "Error while sleeping");
		} // wait for all actors to be created
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
			waitBeforeTerminate(actorList);
		} catch (InterruptedException ex) {
			ex.printStackTrace();
		} finally {
			compute_stats(actorList, f, N);
			system.terminate();
		}
	}

	public static void waitBeforeTerminate(ArrayList<ActorRef> actorList) throws InterruptedException {
		Scanner scanner = new Scanner(System.in);
		System.out.println("Type 'q' to end program.");
		while (scanner.nextLine().compareTo("q") != 0) {
			Thread.sleep(1000);
		}
		scanner.close();
	}

	public static void compute_stats(ArrayList<ActorRef> actorList, int f, int N){
		long total_write_waiting = 0;
		long total_read_waiting = 0;
		long total_time_running = 0;

		long minStartTime = Long.MAX_VALUE;
		long maxEndTime = Long.MIN_VALUE;

		for(Process p: Process.getProcessList()){
			total_write_waiting += p.getWriteWaitingTime();
			total_read_waiting += p.getReadWaitingTime();

			if (p.getStartTime() < minStartTime && p.getStartTime() != 0) {
				minStartTime = p.getStartTime();
			}
			if (p.getEndTime() > maxEndTime) {
				maxEndTime = p.getEndTime();
			}
		}
		total_time_running = maxEndTime - minStartTime;

		File file = new File("stats.txt");
		FileWriter fw = null;
		try {
			if (!file.exists()) {
				file.createNewFile();
			}
			fw = new FileWriter(file.getAbsoluteFile(), true); // Open in append mode

			fw.append("Run N = "+N+" f = "+f+", M = " +  M +"\n");
			fw.append("Cumulated  write waiting time for all processes = "+total_write_waiting/1000.0+" s \n");
			fw.append("Cumulated read waiting time for all processes = "+total_read_waiting/1000.0+" s \n");
			fw.append("Total time running for all processes = "+total_time_running/1000.0+" s\n");

			fw.append("Average write waiting time per process = "+(total_write_waiting/N)+" ms\n");
			fw.append("Average read waiting time per process = "+(total_read_waiting/N)+" ms\n");
			fw.append("Average time running per process = "+(total_time_running/N)+" ms\n");

			fw.append("Average write waiting time per operation = "+(total_write_waiting/(N*Process.M))+" ms\n");
			fw.append("Average read waiting time per operation = "+(total_read_waiting/(N*Process.M))+" ms\n");
			fw.append("Average time running per operation = "+(total_time_running/(N*Process.M))+" ms\n");

			fw.append("Throughput = "+((N*Process.M)/(total_time_running/1000.0))+" op/s\n");

			fw.append("\n\n\n\n");

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
