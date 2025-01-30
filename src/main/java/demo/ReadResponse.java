package demo;

public class ReadResponse {
    
		public final int sequenceNumber;
        public final int localValue;
        public final int localTS;

		public ReadResponse(int localValue, int localTS,int sequenceNumber) {
            this.localValue = localValue;
            this.localTS = localTS;
            this.sequenceNumber = sequenceNumber;
		}
	 }
