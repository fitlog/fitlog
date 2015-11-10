package com.tuoved.app.utils;

public class ExerciseData {
	private int repeats;
	private long relax;
	private float weight;
	
	public ExerciseData(float weight, int repeats, long relax){
		this.weight = weight;
		this.repeats = repeats;
		this.relax = relax;
	}
	
	public ExerciseData(ExerciseData data) {
		this(data.weight(), data.repeats(), data.relax());
	}
	
	public ExerciseData() {
		this.weight = 0;
		this.repeats = 0;
		this.relax = 0;
	}
	
	public void setWeight(float weight) {
		this.weight = weight;
	}
	
	public float weight() {
		return this.weight;
	}
	
	public void setRepeats(int repeats) {
		this.repeats = repeats;
	}
	
	public int repeats() {
		return this.repeats;
	}
	
	public void setRelax(long relax) {
		this.relax = relax;
	}
	
	public long relax() {
		return this.relax;
	}
	
	@Override
	public boolean equals(Object o) {
		if(o instanceof ExerciseData) {
			ExerciseData d = ((ExerciseData)o);
			boolean isEqual = (d.repeats == repeats() && d.relax == relax() && d.weight == weight());
			return isEqual;
		}
		return false;
	}
}
