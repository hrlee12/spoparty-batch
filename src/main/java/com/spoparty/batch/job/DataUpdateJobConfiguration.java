package com.spoparty.batch.job;

import com.spoparty.batch.step.SeasonLeagueTeamPlayerStepConfiguration;
import com.spoparty.batch.step.StandingStepConfiguration;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.spoparty.batch.step.SeasonLeagueJpaStepConfiguration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataUpdateJobConfiguration {


	@Bean
	public Job jpaJob(JobRepository jobRepository, Step seasonLeagueStep, Step seasonLeagueTeamStep, Step seasonLeagueTeamPlayerStep, Step standingStep, Step fixtureStep) {
		return new JobBuilder("jpaJob", jobRepository)
			.start(seasonLeagueStep)
			.next(seasonLeagueTeamStep)
			.next(seasonLeagueTeamPlayerStep)
			.next(standingStep)
			.next(fixtureStep)
				.build();


	}


}
