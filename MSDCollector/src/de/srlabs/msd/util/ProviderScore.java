package de.srlabs.msd.util;

public class ProviderScore 
{
	private float score;
	private Boolean is2G;
	
	public ProviderScore (float score, Boolean is2G)
	{
		this.score = score;
		this.is2G = is2G;
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
}
