package demo;

public class Gather {
	private int senderId;
	private int ballot;
	private int estimateBallot;
	private int estimate;
	
	public Gather(int senderId, int ballot, int estimateBallot, int estimate) {
		this.senderId = senderId;
		this.ballot = ballot;
		this.estimateBallot = estimateBallot;
		this.estimate = estimate;

	}
	
	
	public int getSenderId() {
		return senderId;
	}


	public int getBallot() {
		return ballot;
	}


	public int getEstimateBallot() {
		return estimateBallot;
	}


	public int getEstimate() {
		return estimate;
	}

	
	
	
}
