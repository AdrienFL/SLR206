package demo;

public class WriteAck {

        public final String ack;
		public final int value;
		public final int timestamp;
        
		public WriteAck(String ack, int value, int timestamp) {
            this.ack = ack;
			this.value = value;
			this.timestamp = timestamp;
		}
	 }
