package de.srlabs.msd.util;

public class ProviderScore 
{
	private float score;
	private Boolean is2G;
	private int color;
	
	public ProviderScore (float score, Boolean is2G, int color)
	{
		this.score = score;
		this.is2G = is2G;
		this.color = color;
	}

	public float getScore() {
		return score;
	}

	public Boolean getIs2G() {
		return is2G;
	}

	public void setScore(float score) {
		this.score = score;
	}

	public void setIs2G(Boolean is2g) {
		is2G = is2g;
	}

	public int getColor() {
		return color;
	}

	public void setColor(int color) {
		this.color = color;
	}
}
