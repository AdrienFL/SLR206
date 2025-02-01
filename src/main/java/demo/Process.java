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

	private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	
	private ArrayList<ActorRef> knownActors;
	
	public int localValue;
	public int localTS;
	public int t;
	public int r;
	private int readResponseCounter;
	private int ackCounter;
	private ArrayList<ReadResponse> readResponseList;
	private final int N = 3;
	private final int M = 3;
	private int rw;
	private int opNumber;
	private ArrayList<Integer> valueToWrite;

	
	public Process() {
		this.knownActors = new ArrayList<ActorRef>();
		this.localTS = 0;
		this.localValue = 0;
		this.t = 0;
		this.r = 0;
		this.readResponseCounter = 0;
		this.ackCounter = 0;
		this.readResponseList = new ArrayList<ReadResponse>();
		this.rw = 1;
		this.opNumber = 0;
		this.valueToWrite = new ArrayList<Integer>();

		
	}


	
	public static Props createActor() {
		return Props.create(Process.class, () -> {
			return new Process();
		});
	}


	public void read(){
		this.rw= 0;
		this.r++;
		ReadRequest m = new ReadRequest(this.r);
		for (ActorRef a : knownActors){
			a.tell(m, this.getSelf());
		}
	}
	
	
	public void write(int value){
		
		this.r++;
		ReadRequest m = new ReadRequest(this.r);
		
		for (ActorRef a : knownActors){
			a.tell(m, this.getSelf());
		}

	}

	@Override
	public void onReceive(Object message) throws Throwable {
		if(message instanceof ActorRef){
			ActorRef actorRef = (ActorRef) message;
			
			this.knownActors.add(actorRef);			

			log.info("["+getSelf().path().name()+"] received Reference of "+ actorRef.path().name() +" from ["+ getSender().path().name() +"]");

		}
		if (message instanceof MyMessage){
			MyMessage m = (MyMessage) message;
			log.info("["+getSelf().path().name()+"] received message from "+ getSender().path().name() +" with data ["+ m.data +"]");

			if (m.data.equals("launch")){
				for(int i =0; i<M; i++){
					int v = i*N + Integer.parseInt(getSelf().path().name());
					valueToWrite.add(v);
					
				}
				log.info("[Write] : Operation : ["+ this.opNumber + "] , value : " + this.valueToWrite.get(this.opNumber));
				this.write(this.valueToWrite.get(0));
			}
			if(m.data.equals("crash")){
				while (true) { 
					Thread.sleep(10000);
				}
			}
			if(m.data.equals("next")){
				this.ackCounter =0;
				this.readResponseCounter = 0;
				if(this.opNumber <2*M-1 && this.opNumber > M-2){
					this.rw = 0;
					this.opNumber++;
					log.info("[DEBUG] opNumber :" + this.opNumber);

					log.info("[Read] : "+ (this.opNumber - M +1));
					this.read();

				}
				if(this.opNumber<M-1){
					
					this.rw = 1;
					this.opNumber++;
					log.info("[DEBUG] opNumber :" + this.opNumber);
					log.info("[Write] : Operation : ["+ this.opNumber + "] , value : " + this.valueToWrite.get(this.opNumber));
					this.write(this.valueToWrite.get(this.opNumber));
				}
				
			}
		}
		
		
		
		if(message instanceof ReadRequest){
			ReadRequest m = (ReadRequest) message;
			log.info("["+getSelf().path().name()+"] received read request from ["+ getSender().path().name() +"] with sequence number : [" + m.sequenceNumber+"]");
			ReadResponse res = new ReadResponse(localValue, localTS, r);
			getSender().tell(res, getSelf());
		}	

		if(message instanceof WriteRequest){
			WriteRequest m = (WriteRequest) message;
			log.info("["+getSelf().path().name()+"] received write request from ["+ getSender().path().name() +"] with value : [" + m.value+"] and timestamp : ["+m.timestamp+"]");
			if (m.timestamp > this.localTS || (m.timestamp == this.localTS && m.value > localValue)) {
				this.localValue = m.value;
				this.localTS = m.timestamp;
			}
			WriteAck ack = new WriteAck("ack", m.value, m.timestamp);
			getSender().tell(ack, getSelf());
		}

		if(message instanceof ReadResponse){
			ReadResponse m = (ReadResponse) message;
			log.info("["+getSelf().path().name()+"] received read response from ["+ getSender().path().name() +"] with local TS : [" + m.localTS+"] and value : ["+m.localValue+"], and sequence number : [" + m.sequenceNumber+ "]");
			if (this.r == m.sequenceNumber){

				this.readResponseCounter++;
				this.readResponseList.add(m);
				if(this.readResponseCounter >= N/2){
					if(rw == 0){
						this.readResponseCounter = 0;
						int maxTimestamp = 0;
						for(ReadResponse r : this.readResponseList){
							if(r.localTS > maxTimestamp){
								maxTimestamp = r.localTS;
								this.localValue = r.localValue;
							}
							if(r.localTS == maxTimestamp){
								if(this.localValue < r.localValue){
									this.localValue = r.localValue;
								}
							}
						}
						this.ackCounter = 0;
						this.readResponseList.clear();
						WriteRequest request = new WriteRequest(this.localValue, this.t);
						for (ActorRef a : knownActors){
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
						this.readResponseCounter = 0;
						this.readResponseList.clear();
						this.t = maxTimestamp + 1;
						WriteRequest request = new WriteRequest(this.valueToWrite.get(this.opNumber), this.t);
						for (ActorRef a : knownActors){
							a.tell(request, this.getSelf());
						}
					}
				}
			}
			
		}

		if(message instanceof WriteAck){
			WriteAck m = (WriteAck) message;
			log.info("["+getSelf().path().name()+"] received write ack from ["+ getSender().path().name() +"] with value : [" + m.value+"] and timestamp : ["+m.timestamp+"]");
			if(this.t == m.timestamp && this.localValue == m.value){
				this.ackCounter++;
				//log.info("[DEBUG] "+ this.ackCounter);
				if(this.ackCounter > N/2){
					this.ackCounter = 0;
					if(this.rw == 0){
						log.info("[Read] [Return] Operation : "+ (this.opNumber - M+1) + " , value : ["+this.localValue + "]");
					}
					if(this.rw == 1){
						log.info("[Write] [Return] Operation : " + this.opNumber  +" , value : ok");
					}
					MyMessage nextOperationMessage = new MyMessage("next", "0");
					getContext().system().scheduler().scheduleOnce(Duration.ofMillis(0), getSelf(), nextOperationMessage, getContext().system().dispatcher(), ActorRef.noSender());

					}
				}

			}
		}
	}



