package demo;

public class State {
	private int estimate;
	private int estimateBallot;
	
	public State(int estimate, int estimateBallot) {
		this.estimate = estimate;
		this.estimateBallot = estimateBallot;
	}
	
	public int getEstimate() {
		return estimate;
	}
	public int getEstimateBallot() {
		return estimateBallot;
	}
	
	
}
