package de.srlabs.snoopsnitch.analysis;

public class Score {

	private int year = 0;
	private int month = 0;
	private double score = 0.0;

	Score (int year, int month, double score) {
		this.year = year;
		this.month = month;
		this.score = score;
	}

	public int getYear() {
		return year;
	}

	public int getMonth() {
		return month;
	}

	public double getScore() {
		return score;
	}
}
