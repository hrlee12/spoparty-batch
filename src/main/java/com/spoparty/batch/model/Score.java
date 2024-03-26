package com.spoparty.batch.model;

import lombok.Data;

@Data
public class Score {
	private ScoreDetail halftime;
	private ScoreDetail fulltime;
	private ScoreDetail extratime;
	private ScoreDetail penalty;

}
