package com.spoparty.batch.model;

import java.util.List;

import lombok.Data;

@Data
public class Lineups {

	Team team;
	Coaches coach;
	String formation;
	List<LineupPlayers> startXI;
	List<LineupPlayers> substitutes;

}
