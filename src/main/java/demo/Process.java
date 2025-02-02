package demo;
import java.lang.reflect.Array;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedAbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class Process extends UntypedAbstractActor {

	private static ArrayList<Process> processList = new ArrayList<Process>();
	private static final int N = SLR206.N;
	public static final int M = SLR206.M;

	private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

	private ArrayList<ActorRef> knownActors;

	public int localValue;
	public boolean isCrashed = false;
	public int localTS;
	public int t;
	public int r;
	private HashMap<Integer, Integer> readResponseCounter;
	private HashMap<Integer, Integer> ackCounter;
	private ArrayList<ReadResponse> readResponseList;
	private int rw;
	private int opNumber;
	private ArrayList<Integer> valueToWrite;
	private int number_of_waiting_state;
	private long r_start_time, r_end_time, w_start_time, w_end_time, run_start_time, run_end_time;

	private long r_waiting_time, w_waiting_time, running_time;

	private boolean isDone = false;


		public Process() {
			this.knownActors = new ArrayList<ActorRef>();
			this.localTS = 0;
			this.localValue = 0;
			this.t = 0;
			this.r = 0;
			this.readResponseCounter = new HashMap<Integer, Integer>();
			this.ackCounter = new HashMap<Integer, Integer>();
			this.readResponseList = new ArrayList<ReadResponse>();
			this.rw = 1;
			this.opNumber = 0;
			this.valueToWrite = new ArrayList<Integer>();
			this.number_of_waiting_state = 0;
			this.r_waiting_time = 0;
			this.w_waiting_time = 0;
			this.r_start_time = 0;
			this.r_end_time = 0;
			this.w_start_time = 0;
			this.w_end_time = 0;
			processList.add(this);


		}



		public static Props createActor() {
			return Props.create(Process.class, Process::new);
		}

		public static ArrayList<Process> getProcessList() {
			return processList;
		}

		public boolean isDone(){
			return this.isDone;
		}


		public void read(){
			r_start_time = System.currentTimeMillis();
			this.rw= 0;
			this.r++;
			ReadRequest m = new ReadRequest(this.r);
			for (ActorRef a : knownActors){
				log.info("Process ["+getSelf().path().name()+"] sending read request to process ["+ a.path().name() +"] with sequence number : [" + this.r+"]");
				a.tell(m, this.getSelf());
			}
		}


		public void write(int value){
			this.rw = 1;
			this.r++;
			ReadRequest m = new ReadRequest(this.r);

			w_start_time = System.currentTimeMillis();
			for (ActorRef a : knownActors){
				log.info("Process ["+getSelf().path().name()+"] sending read request to process ["+ a.path().name() +"] with sequence number : [" + this.r+"]");
				a.tell(m, this.getSelf());
			}

		}

		@Override
		public void onReceive(Object message) throws Throwable {
			if (isCrashed){
				return;
			}
			if(message instanceof ActorRef){
				ActorRef actorRef = (ActorRef) message;

				this.knownActors.add(actorRef);

				log.info("Process ["+getSelf().path().name()+"] received Reference of process "+ actorRef.path().name() +" from process ["+ getSender().path().name() +"]");

			}
			if (message instanceof MyMessage){
				MyMessage m = (MyMessage) message;
				log.info("Process ["+getSelf().path().name()+"] received message from process "+ getSender().path().name() +" with data ["+ m.data +"]");

				if (m.data.equals("launch")){
					run_start_time = System.currentTimeMillis();
					this.ackCounter.put(this.opNumber, 0);
					this.readResponseCounter.put(this.opNumber, 0);
					for(int i =0; i<M; i++){
						int v = i*N + Integer.parseInt(getSelf().path().name());
						valueToWrite.add(v);

					}
					log.info("[Write] : Operation : ["+ this.opNumber + "] , value : " + this.valueToWrite.get(this.opNumber));
					this.write(this.valueToWrite.get(0));
				}
				if(m.data.equals("crash")){
					log.info("Process ["+getSelf().path().name()+"] is crashing");
					log.info("Process ["+getSelf().path().name()+"] is done");
					this.isDone = true;
					run_end_time = 0;
					isCrashed = true;
				}
				if(m.data.equals("next")){
					this.opNumber++;
					log.info("[DEBUG] opNumber :" + this.opNumber);

					this.ackCounter.put(this.opNumber, 0);
					this.readResponseCounter.put(this.opNumber, 0);

					if(this.opNumber<=M-1){

						this.rw = 1;

						log.info("[Write] : Operation : ["+ this.opNumber + "] , value : " + this.valueToWrite.get(this.opNumber));
						this.write(this.valueToWrite.get(this.opNumber));
					}
					else{
						if (this.opNumber <=2*M-1){
							this.rw = 0;
							log.info("[Read] : "+ (this.opNumber - M));
							this.read();
						} else {
							log.info("Process ["+getSelf().path().name()+"] is done");
							this.isDone = true;
							run_end_time = System.currentTimeMillis();
						}
					}

				}
			}



			if(message instanceof ReadRequest){
				ReadRequest m = (ReadRequest) message;
				log.info("Process ["+getSelf().path().name()+"] received read request from process ["+ getSender().path().name() +"] with sequence number : [" + m.sequenceNumber+"]");
				ReadResponse res = new ReadResponse(localValue, localTS, r);
				log.info("Process ["+getSelf().path().name()+"] sending read response to process ["+ getSender().path().name() +"] with local TS : [" + localTS+"] and value : ["+localValue+"], and sequence number : [" + r+ "]");
				getSender().tell(res, getSelf());
			}

			if(message instanceof WriteRequest){
				WriteRequest m = (WriteRequest) message;
				log.info("Process ["+getSelf().path().name()+"] received write request from process ["+ getSender().path().name() +"] with value : [" + m.value+"] and timestamp : ["+m.timestamp+"]");
				if (m.timestamp > this.localTS || (m.timestamp == this.localTS && m.value > localValue)) {
					this.localValue = m.value;
					this.localTS = m.timestamp;
				}
				WriteAck ack = new WriteAck("ack", m.value, m.timestamp);
				log.info("Process ["+getSelf().path().name()+"] sending write ack to process ["+ getSender().path().name() +"] with value : [" + m.value+"] and timestamp : ["+m.timestamp+"]");
				getSender().tell(ack, getSelf());
			}

			if(message instanceof ReadResponse){
				ReadResponse m = (ReadResponse) message;
				log.info("Process ["+getSelf().path().name()+"] received read response from process ["+ getSender().path().name() +"] with local TS : [" + m.localTS+"] and value : ["+m.localValue+"], and sequence number : [" + m.sequenceNumber+ "]");

					this.readResponseCounter.put(this.opNumber, this.readResponseCounter.get(this.opNumber) + 1);
					this.readResponseList.add(m);
					if(this.readResponseCounter.get(this.opNumber) > N/2){
						if(rw == 0){
							this.readResponseCounter.put(this.opNumber, 0);
							int maxTimestamp = 0;
							int maxValue = 0;
							for(ReadResponse r : this.readResponseList){
								if(r.localTS > maxTimestamp){
									maxTimestamp = r.localTS;
									maxValue = r.localValue;
								}
								if(r.localTS == maxTimestamp){
									if(this.localValue < r.localValue){
										maxValue = r.localValue;
									}
								}
							}
							this.ackCounter.put(this.opNumber, 0);
							this.readResponseList.clear();
							WriteRequest request = new WriteRequest(maxValue, maxTimestamp);
							for (ActorRef a : knownActors){
								log.info("Process ["+getSelf().path().name()+"] sending write request to process ["+ a.path().name() +"] with value : [" + maxValue+"] and timestamp : ["+maxTimestamp+"]");
								a.tell(request, this.getSelf());
							}
						}

						if(rw == 1){
							int maxTimestamp = 0;
							for(ReadResponse r : this.readResponseList){
								if(r.localTS > maxTimestamp){
									maxTimestamp = r.localTS;
								}
							}
							this.readResponseCounter.put(this.opNumber, 0);
							this.readResponseList.clear();
							this.t = maxTimestamp + 1;
							WriteRequest request = new WriteRequest(this.valueToWrite.get(this.opNumber), this.t);
							for (ActorRef a : knownActors){
								log.info("Process ["+getSelf().path().name()+"] sending write request to process ["+ a.path().name() +"] with value : [" + this.valueToWrite.get(this.opNumber)+"] and timestamp : ["+this.t+"]");
								a.tell(request, this.getSelf());
							}
						}
					}

			}

			if(message instanceof WriteAck){
				WriteAck m = (WriteAck) message;
				log.info("Process ["+getSelf().path().name()+"] received write ack from process ["+ getSender().path().name()+"] with value : [" + m.value+"] and timestamp : ["+m.timestamp+"]");
					this.ackCounter.put(this.opNumber, this.ackCounter.get(this.opNumber) + 1);
					//log.info("[DEBUG] "+ this.ackCounter);
					if(this.ackCounter.get(this.opNumber) > N/2 ){
						this.ackCounter.put(this.opNumber, 0);
						if(this.rw == 0){
						r_end_time = System.currentTimeMillis();
						this.r_waiting_time += r_end_time - r_start_time;
						log.info("[Read] [Return] Operation : "+ (this.opNumber - M+1) + " , value : ["+this.localValue + "]");
					}
					if(this.rw == 1){
						w_end_time = System.currentTimeMillis();
						this.w_waiting_time += w_end_time - w_start_time;
						log.info("[Write] [Return] Operation : " + this.opNumber  +" , value : ok");
						log.debug("[Debug] localValue :" + this.localValue);
					}
					MyMessage nextOperationMessage = new MyMessage("next", "0");
					getContext().system().scheduler().scheduleOnce(Duration.ofMillis(0), getSelf(), nextOperationMessage, getContext().system().dispatcher(), ActorRef.noSender());

					}
					log.info("Wainting for acks : "+ this.ackCounter.get(this.opNumber));
				}

		}

		public long getReadWaitingTime(){
			return this.r_waiting_time;
		}

		public long getWriteWaitingTime(){
			return this.w_waiting_time;
		}

		public long getRunningTime(){
			return this.run_end_time - this.run_start_time;
		}

		public long getStartTime(){
			return this.run_start_time;
		}

		public long getEndTime(){
			return this.run_end_time;
		}

	}



