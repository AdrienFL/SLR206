package demo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedAbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import java.util.LinkedList;

public class Process2 extends UntypedAbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private ArrayList<ActorRef> knownActors;
    private boolean isDone = false;
    private int localValue = 0; // locally stored value, initially 0
    private int localTS = 0; // locally stored timestamp, initially 0
    private int t = 0; // timestamp
    private int r = 0; // sequence number (the number of issued read requests)
    private LinkedList<ReadResponse> readResponses = new LinkedList<>(); // list of read responses
    private int writeAck = 0; // number of write acks received

    public Process2() {
        this.knownActors = new ArrayList<>();
    }

    public static Props createActor() {
        return Props.create(Process2.class, Process2::new);
    }

    @Override
    public void onReceive(Object message) throws Throwable {
        if (message instanceof String) {
            String msg = (String) message;
            if (msg.equals("crash")) {
                log.info("Crashing...");
                getContext().stop(getSelf());
            } else if (msg.equals("launch")) {
                log.info("Launching...");
            }
        } else if (message instanceof ActorRef) {
            log.info("Process {} received ActorRef {} from {}", getSelf(), message, getSender());
            ActorRef actor = (ActorRef) message;
            knownActors.add(actor);
        } else if (message instanceof ReadRequest)
            handleReadRequest((ReadRequest) message);
        else if (message instanceof ReadResponse)
            handleReadResponse((ReadResponse) message);
        else if (message instanceof WriteRequest)
            handleWriteRequest((WriteRequest) message);
        else if (message instanceof WriteAck)
            handleWriteResponse((WriteAck) message);
    }

    //send local value and timestamp to the sender
    private void handleReadRequest(ReadRequest message) {
        log.info("Process {} received ReadRequest {} from {}", getSelf(), message, getSender());
        int value = localValue;
        int timestamp = localTS;
        ReadResponse response = new ReadResponse(value, timestamp, r);
        getSender().tell(response, getSelf());
    }

    private void handleReadResponse(ReadResponse message) {
        log.info("Process {} received ReadResponse {} from {}", getSelf(), message, getSender());
        readResponses.add(message);

    }

    private void handleWriteRequest(WriteRequest message) {
        log.info("Process {} received WriteRequest {} from {}", getSelf(), message, getSender());
        int value = message.value;
        int timestamp = message.timestamp;
        if (timestamp > localTS || (timestamp == localTS && message.value > localValue)) {
            localValue = value;
            localTS = timestamp;
        }
        WriteAck ack = new WriteAck("ack", r, timestamp);
        getSender().tell(ack, getSelf());

    }

    private void handleWriteResponse(WriteAck message) {
        log.info("Process {} received WriteAck {} from {}", getSelf(), message, getSender());
        writeAck++;
    }

}
