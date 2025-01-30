package demo;
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
	public int readResponseCounter;
	public int ackCounter;
	public ArrayList<ReadResponse> readResponseList;
	private final int N = 10;
	private final int M = 5;

	public Process() {
		this.knownActors = new ArrayList<ActorRef>();
		this.localTS = 0;
		this.localValue = 0;
		this.t = 0;
		this.r = 0;
		this.readResponseCounter = 0;
		this.ackCounter = 0;
		this.readResponseList = new ArrayList<ReadResponse>();

		
	}


	
	public static Props createActor() {
		return Props.create(Process.class, () -> {
			return new Process();
		});
	}


	public int read(){
		this.r++;
		ReadRequest m = new ReadRequest(this.r);
		for (ActorRef a : knownActors){
			a.tell(m, this.getSelf());
		}
		while(this.readResponseCounter < N/2){
			try {
				Thread.sleep(10);
			} catch (InterruptedException ex) {
				ex.printStackTrace();
			}
		}
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
		return this.localValue;
	}
	
	
	public boolean write(int value){
		this.r++;
		ReadRequest m = new ReadRequest(this.r);
		for (ActorRef a : knownActors){
			a.tell(m, this.getSelf());
		}
		while(this.readResponseCounter < N/2){
			try {
				Thread.sleep(10);
			} catch (InterruptedException ex) {
				ex.printStackTrace();
			}
		}
		int maxTimestamp = 0;
		for(ReadResponse r : this.readResponseList){
			if(r.localTS > maxTimestamp){
				maxTimestamp = r.localTS;
			}
		}
		this.readResponseCounter = 0;
		this.readResponseList.clear();
		this.t = maxTimestamp + 1;
		WriteRequest request = new WriteRequest(this.localValue, this.t);
		for (ActorRef a : knownActors){
			a.tell(request, this.getSelf());
		}
		while(this.ackCounter < N/2){
			try {
				Thread.sleep(10);
			} catch (InterruptedException ex) {
				ex.printStackTrace();
			}
		}
		this.ackCounter = 0;

		return true;
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
			if (m.data.equals("launch")){
				for(int i =0; i<M; i++){
					this.write(i*N + Integer.parseInt(getSelf().path().name()));
				}
			}
			if(m.data.equals("crash")){
				while (true) { 
					Thread.sleep(10000);
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
			log.info("["+getSelf().path().name()+"] received write request from ["+ getSender().path().name() +"] with local TS : [" + m.localTS+"] and value : ["+m.localValue+"], and sequence number : [" + m.sequenceNumber+ "]");
			if (this.r == m.sequenceNumber){
				this.readResponseCounter++;
				this.readResponseList.add(m);
			}
		}

		if(message instanceof WriteAck){
			WriteAck m = (WriteAck) message;
			log.info("["+getSelf().path().name()+"] received write request from ["+ getSender().path().name() +"] with value : [" + m.value+"] and timestamp : ["+m.timestamp+"]");
			if(this.t == m.timestamp && this.localValue == m.value){
				this.ackCounter++;

			}
		}
	}
}


