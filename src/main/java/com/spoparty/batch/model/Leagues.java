package com.spoparty.batch.model;

import java.util.List;


import lombok.Data;

@Data
public class Leagues {

	private League league;
	private Country country;
	private List<Season> seasons;
}
