package com.spoparty.batch.model;

import java.util.Map;

import lombok.Data;

@Data
public class Events {

	Map<String, String> time;
	Team team;
	Player player;
	Player assist;
	String type;
	String detail;
	Object comments;

}
