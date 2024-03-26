package com.spoparty.batch.util;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.spoparty.batch.model.Response;
import com.spoparty.batch.model.Standing;
import com.spoparty.batch.model.StandingLeague;
import com.spoparty.batch.entity.*;
import com.spoparty.batch.entity.Fixture;
import com.spoparty.batch.entity.League;
import com.spoparty.batch.entity.Team;
import com.spoparty.batch.model.*;
import com.spoparty.batch.repository.*;
import com.spoparty.batch.scheduler.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import com.spoparty.batch.Exception.ApiWrongDataResponseException;

@Component
@RequiredArgsConstructor
@Slf4j
public class EntityParser {

	private final SeasonLeagueTeamPlayerRepository seasonLeagueTeamPlayerRepository;
	private final TeamRepository teamRepository;
	private final StandingRepository standingRepository;
	private final SeasonLeagueTeamRepository seasonLeagueTeamRepository;
	private final FixtureRepository fixtureRepository;


	public SeasonLeague seasonLeagueParser(Long seasonLeagueId, LeagueResponse leagueResponse) {
		Leagues leagues = leagueResponse.getResponse().get(0);
		League league = leagues.getLeague();
		Country country = leagues.getCountry();

		com.spoparty.batch.model.Season season = null;
		for (com.spoparty.batch.model.Season s: leagues.getSeasons()){
			if (s.getYear() == 2023) {
				season = s;
				break;
			}
		}

		com.spoparty.batch.entity.League leagueEntity = com.spoparty.batch.entity.League.builder()
			.id(league.getId())
			.nameKr(league.getName())
			.nameEng(league.getName())
			.logo(league.getLogo())
			.country(country.getName())
			.countryLogo(country.getFlag())
			.type(league.getType())
			.build();

		com.spoparty.batch.entity.Season seasonEntity = com.spoparty.batch.entity.Season.builder()
			.id(2023)
			.value("2023")
			.build();


		return SeasonLeague.builder()
			.id(seasonLeagueId)
			.league(leagueEntity)
			.season(seasonEntity)
			.seasonStartDate(ToLocalDateTime((String)season.getStart()))
			.seasonEndDate(ToLocalDateTime((String)season.getEnd()))
			.build();

	}

	public SeasonLeagueTeam seasonLeagueTeamParser(SeasonLeagueTeam item, TeamResponse teamResponse, CoachResponse coachResponse) {
		if (teamResponse == null || coachResponse == null)
			return null;




		Team beforeTeam = item.getTeam();
		com.spoparty.batch.model.Team teamInfo = teamResponse.getResponse().get(0).getTeam();
		if (beforeTeam.getId() != teamInfo.getId())
			throw new ApiWrongDataResponseException("잘못된 팀 정보를 가져왔습니다.");


		Team afterTeam = Team.builder()
			.id(teamInfo.getId())
			.nameKr(teamInfo.getName())
			.nameEng(teamInfo.getName())
			.logo(teamInfo.getLogo())
			.build();

		List<Coaches> coaches = coachResponse.getResponse();

		Coach afterCoach = null;
		coachLoop:
			for (Coaches coach : coaches) {
				for (Career career : coach.getCareer()) {
					// if (career.getTeam().getId() != beforeTeam.getId())
					// 	continue;
					if (career.getEnd() == null || isCurrentCoach(career.getStart(), career.getEnd())) {
						afterCoach = Coach.builder()
							.id(coach.getId())
							.photo(coach.getPhoto())
							.nationality(coach.getNationality())
							.age(coach.getAge())
							.nameKr(coach.getName())
							.nameEng(coach.getName())
							.build();
					}
				}
			}
		Boolean changeTeam;
		SeasonLeagueTeam afterSeasonLeagueTeam = null;

		// 팀 정보가 바뀌지 않은 경우
		if (!changeTeamInfo(beforeTeam, afterTeam)) {
			changeTeam = false;
		// 팀 정보가 바뀐 경우
		} else {
			changeTeam = true;
		}

		// 현재 코치가 없는 경우
		if (afterCoach == null) {

		// 현재 코치가 기존 코치와 동일인물인 경우
		} else if (item.getCoach().getId() == afterCoach.getId()){
			// 코치의 세부 정보가 달라졌다면
			if (changeCoachInfo(item.getCoach(), afterCoach)){

			// 코치의 세부정보가 그대로라면
			} else {
				// 팀도 변경사항이 없다면 전부 그대로이므로 SeasonLeagueTeam 수정하지 않는다.
				if (!changeTeam)

					return null;
			}
			// 현재 코치가 기존 코치와 다른 인물인 경우
		} else {
			// 엔티티에서 코치 정보 수정.
			item.changeCoach(afterCoach);
		}

		afterSeasonLeagueTeam = SeasonLeagueTeam.builder()
			.seasonLeague(item.getSeasonLeague())
			.team(afterTeam)
			.coach(afterCoach)
			.build();




		afterSeasonLeagueTeam.setId(item.getId());


		return afterSeasonLeagueTeam;


	}

	public List<SeasonLeagueTeamPlayer> seasonLeagueTeamPlayerParser(SeasonLeagueTeam item, List<com.spoparty.batch.model.Player> players) {
		List<SeasonLeagueTeamPlayer> beforePlayers = item.getSeasonLeagueTeamPlayers();
		List<SeasonLeagueTeamPlayer> changePlayers = new ArrayList<>();


		for (com.spoparty.batch.model.Player player : players) {
			boolean isCatch = false;
			for (SeasonLeagueTeamPlayer beforePlayer : beforePlayers) {
				if (beforePlayer.getPlayer().getId() != player.getId()) continue;

				isCatch = true;
				Long seasonLeagueTeamPlayerId = beforePlayer.getId();
				beforePlayers.remove(beforePlayer);
				// 기존의 선수의 정보가 바꼈으면 추가.
				if (changePlayerInfo(beforePlayer.getPlayer(), player)) {

					changePlayers.add(makeSeasonLeagueTeamPlayer(item, player, seasonLeagueTeamPlayerId));

				}

					break;

			}

			if (isCatch) continue;


			// 기존 선수중에 선수가 없었다면
			changePlayers.add(makeSeasonLeagueTeamPlayer(item, player, null));


		}

		for (SeasonLeagueTeamPlayer removePlayer : beforePlayers) {
			seasonLeagueTeamPlayerRepository.delete(removePlayer);
		}

		return changePlayers;
	}




	public List<Standings> standingParser(SeasonLeague item, Response response) {
		StandingLeague standingLeague = response.getResponse().get(0).getLeague();

		if (standingLeague.getId() != item.getLeague().getId()) {
			throw new ApiWrongDataResponseException("다른 리그의 순위 정보를 받았습니다.");
		}

		List<List<Standing>> standingList = standingLeague.getStandings();

		log.info("standingList size" + standingList.size());


		List<Standings> result = new ArrayList<>();


		for (int i = 0; i < standingList.size(); i++) {
			List<Standing> standings = standingList.get(i);


			for (Standing standing: standings) {


				SeasonLeagueTeam seasonLeagueTeam = seasonLeagueTeamRepository.findByTeam_IdAndSeasonLeague(standing.getTeam().getId(), item);

				Standings beforeStanding = standingRepository.findByGroupAndSeasonLeagueTeam(standing.getGroup(), seasonLeagueTeam);

				Standings afterStanding = null;
				// 저장되지 않은 정보일 때,
				if (beforeStanding == null) {

					afterStanding = makeStanding(standing, seasonLeagueTeam, null);

				// 이미 저장된 정보일 때,
				} else {
					if (changeStandingInfo(beforeStanding, standing)) {
					// 내용이 바꼈을 때,

						afterStanding = makeStanding(standing, seasonLeagueTeam, beforeStanding.getId());
					} else {

						// 내용이 바뀌지 않았을 땐 null 그대로 두기
//						System.out.println("내용 안 바뀜 ");
					}
				}

				if (afterStanding == null) continue;

				result.add(afterStanding);
			}
		}

		return result;
	}

	public List<Fixture> fixtureParser(SeasonLeague item, FixturesResponse fixturesResponse) {

		List<Fixtures> list = fixturesResponse.getResponse();

		List<Fixture> afterFixtures = new ArrayList<>();


		for (Fixtures data : list) {

			SeasonLeagueTeam home = null;
			SeasonLeagueTeam away = null;
			LocalDateTime ldt = null;
			if (data.getTeams().getHome() != null) {
				home = seasonLeagueTeamRepository.findByTeam_IdAndSeasonLeague(data.getTeams().getHome().getId() * 1L, item);
			}
			if (data.getTeams().getAway() != null) {
				away = seasonLeagueTeamRepository.findByTeam_IdAndSeasonLeague(data.getTeams().getHome().getId() * 1L, item);
			}
			if (!data.getFixture().getDate().isEmpty()) {
				OffsetDateTime odt = OffsetDateTime.parse(data.getFixture().getDate());
				ldt = odt.toLocalDateTime().plusHours(9);
			}

			Fixture beforeFixture = fixtureRepository.findById((long)data.getFixture().getId()).orElse(null);

			// 없던 정보일 경우
			if (beforeFixture == null) {
				afterFixtures.add(makeFixture(data, item, home, away, ldt));
			// 기존의 정보이고 정보가 바뀐 경우
			// 정보가 그대로인 경우 데이터 추가하지 않는다.
			} else if (changeFixture(beforeFixture, data, ldt)) {
					afterFixtures.add(makeFixture(data, item, home, away, ldt));
			}
		}

		return afterFixtures;
	}

	private LocalDateTime ToLocalDateTime(String date) {
		String[] dateUnit = date.split("-");

		LocalDate localDate = LocalDate.of(Integer.parseInt(dateUnit[0]),
			Integer.parseInt(dateUnit[1]), Integer.parseInt(dateUnit[2]));

		return LocalDateTime.of(localDate, LocalTime.MIN);
	}


	private boolean isCurrentCoach(String start, String end) {

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		LocalDate current = LocalDate.now(ZoneId.of("Europe/London"));
		LocalDate startTime = LocalDate.parse(start, formatter);
		LocalDate endTime = LocalDate.parse(end, formatter);

		if (current.compareTo(startTime) * current.compareTo(endTime) <= 0 ) return true;
		else return false;
	}


	private boolean changeTeamInfo(Team beforeTeam, Team afterTeam) {
		if (!beforeTeam.getLogo().equals(afterTeam.getLogo()) || !beforeTeam.getNameEng().equals(afterTeam.getNameEng()) || beforeTeam.getNameKr().equals(afterTeam.getNameKr()))
			return true;
		else
			return false;
	}

	private boolean changeCoachInfo(Coach beforeCoach, Coach afterCoach) {
		if (beforeCoach.getAge() != afterCoach.getAge()
			|| !beforeCoach.getNameKr().equals(afterCoach.getNameKr())
			|| !beforeCoach.getNameEng().equals(afterCoach.getNameEng())
			|| !beforeCoach.getNationality().equals(afterCoach.getNationality())
			|| !beforeCoach.getPhoto().equals(afterCoach.getPhoto()))
			return true;
		else
			return false;
	}



	private boolean changePlayerInfo(com.spoparty.batch.entity.Player beforePlayer, com.spoparty.batch.model.Player afterPlayer) {
		if (beforePlayer.getAge() != afterPlayer.getAge()
			|| beforePlayer.getNameKr() != null && !beforePlayer.getNameKr().equals(afterPlayer.getName())
			|| beforePlayer.getNameEng() != null && !beforePlayer.getNameEng().equals(afterPlayer.getName())
			|| beforePlayer.getNationality() != null && !beforePlayer.getNationality().equals(afterPlayer.getNationality())
			|| beforePlayer.getHeight() != null && !beforePlayer.getHeight().equals(afterPlayer.getHeight())
			|| beforePlayer.getWeight() != null && !beforePlayer.getWeight().equals(afterPlayer.getWeight())
			|| beforePlayer.getPhoto() != null && !beforePlayer.getPhoto().equals(afterPlayer.getPhoto()))
			return true;
		else
			return false;
	}


	private SeasonLeagueTeamPlayer makeSeasonLeagueTeamPlayer(SeasonLeagueTeam item, com.spoparty.batch.model.Player player, Long SeasonLeagueTeamPlayerId) {
		com.spoparty.batch.entity.Player newPlayer = com.spoparty.batch.entity.Player.builder()
				.age(player.getAge())
				.height(player.getHeight())
				.photo(player.getPhoto())
				.weight(player.getWeight())
				.id(player.getId())
				.nameEng(player.getName())
				.nameKr(player.getName())
				.nationality(player.getNationality())
				.build();

		SeasonLeagueTeamPlayer result =  SeasonLeagueTeamPlayer.builder()
				.seasonLeagueTeam(item)
				.player(newPlayer)
				.build();

		if (SeasonLeagueTeamPlayerId != null)
			result.setId(SeasonLeagueTeamPlayerId);

		return result;
	}

	private boolean changeStandingInfo(Standings before, Standing after) {
		if (before.getRank() != after.getRank()
		|| before.getPoints() != after.getPoints()
		|| before.getGoalDiff() != after.getGoalsDiff()
		|| before.getForm() != null && !before.getForm().equals(after.getForm())
		|| before.getPlayed() != after.getAll().getPlayed()
		|| before.getWin() != after.getAll().getWin()
		|| before.getDraw() != after.getAll().getDraw()
		|| before.getLose() != after.getAll().getLose()
		|| before.getGoalsFor() != after.getAll().getGoals().get("for")
		|| before.getGoalsAgainst() != after.getAll().getGoals().get("against")) {


			return true;
		}
		else {

			return false;
		}
	}


	private Standings makeStanding(Standing standing, SeasonLeagueTeam team, Long standingId) {
		Standings result = Standings.builder()
				.goalsAgainst(standing.getAll().getGoals().get("against"))
				.group(standing.getGroup())
				.lose(standing.getAll().getLose())
				.rank(standing.getRank())
				.points(standing.getPoints())
				.goalDiff(standing.getGoalsDiff())
				.form(standing.getForm())
				.played(standing.getAll().getPlayed())
				.win(standing.getAll().getWin())
				.draw(standing.getAll().getDraw())
				.goalsFor(standing.getAll().getGoals().get("for"))
				.seasonLeagueTeam(team)
				.build();


		if (standingId != null) {
			result.setId(standingId);
		}

		return result;
	}



	private boolean changeFixture(Fixture before, Fixtures after, LocalDateTime startTime) {

		int homeGoal = 0;
		int awayGoal = 0;
		if (after.getGoals().getHome() !=  null) {
			homeGoal = Integer.parseInt(after.getGoals().getHome());
		}

		if (after.getGoals().getAway() !=  null) {
			awayGoal = Integer.parseInt(after.getGoals().getAway());
		}


		if (before.getStartTime() != null && !before.getStartTime().equals(startTime)
		|| before.getRoundKr() != null && !before.getRoundKr().equals(after.getLeague().getRound())
		|| before.getRoundEng() != null && !before.getRoundEng().equals(after.getLeague().getRound())
		|| before.getHomeTeamGoal() != homeGoal
		|| before.getAwayTeamGoal() != awayGoal
		|| before.getStatus() != null && !before.getStatus().equals(after.getFixture().getStatus().get("long")))
			return true;
		else
			return false;
	}


	private Fixture makeFixture(Fixtures after, SeasonLeague seasonLeague, SeasonLeagueTeam home, SeasonLeagueTeam away, LocalDateTime startTime) {

		int homeGoal = 0;
		int awayGoal = 0;
		if (after.getGoals().getHome() !=  null) {
			homeGoal = Integer.parseInt(after.getGoals().getHome());
		}

		if (after.getGoals().getAway() !=  null) {
			awayGoal = Integer.parseInt(after.getGoals().getAway());
		}



		return Fixture.builder()
				.id((long)after.getFixture().getId())
				.startTime(startTime)
				.roundKr(after.getLeague().getRound())
				.roundEng(after.getLeague().getRound())
				.homeTeamGoal(homeGoal)
				.awayTeamGoal(awayGoal)
				.status(after.getFixture().getStatus().get("long"))
				.homeTeam(home)
				.awayTeam(away)
				.seasonLeague(seasonLeague)
				.build();
	}
}
