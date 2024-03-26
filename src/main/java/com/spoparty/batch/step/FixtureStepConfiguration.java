package com.spoparty.batch.step;

import com.spoparty.batch.Exception.ApiRequestFailedException;
import com.spoparty.batch.entity.Fixture;
import com.spoparty.batch.entity.SeasonLeague;
import com.spoparty.batch.model.FixturesResponse;
import com.spoparty.batch.util.EntityParser;
import com.spoparty.batch.util.FootballApiUtil;
import com.spoparty.batch.writer.JpaItemListWriter;
import com.spoparty.batch.writer.JpaItemListWriterBuilder;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;


@RequiredArgsConstructor
@Slf4j
@Configuration
public class FixtureStepConfiguration {

    private final EntityManagerFactory entityManagerFactory;
    private final FootballApiUtil footballApiUtil;


    private static final int chunkSize = 1;
    private final EntityParser entityParser;

    @Bean
    @JobScope
    public Step fixtureStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("fixtureStep", jobRepository)
                .<SeasonLeague, List<Fixture>>chunk(chunkSize, transactionManager)
                .reader(fixtureSeasonLeaguejpaPagingItemReader())
                .processor(fixtureProcessor())
                .writer(fixtureListWriter())
                .faultTolerant()
                .retry(ApiRequestFailedException.class)
                .retry(DataAccessResourceFailureException.class)
                .retryLimit(3)
                .build();
    }


    @Bean
    public ItemReader<SeasonLeague> fixtureSeasonLeaguejpaPagingItemReader() {
        return new JpaPagingItemReaderBuilder<SeasonLeague>()
                .name("leagueJpaPagingReader")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(chunkSize)
                .queryString("SELECT sl FROM SeasonLeague sl")
                .build();
    }


    @Bean
    public ItemProcessor<SeasonLeague, List<Fixture>> fixtureProcessor() {
        return new ItemProcessor<SeasonLeague, List<Fixture>>() {
            @Override
            public List<Fixture> process(SeasonLeague item) throws Exception {


                MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
                queryParams.add("season", String.valueOf(item.getSeason().getId()));
                queryParams.add("league", String.valueOf(item.getLeague().getId()));

                ResponseEntity<?> response = footballApiUtil.sendRequest("/fixtures", queryParams, FixturesResponse.class);

                if (response.getStatusCode() != HttpStatus.OK){
                    throw new ApiRequestFailedException("API 요청에 실패하였습니다.");
                }

                return entityParser.fixtureParser(item, (FixturesResponse)response.getBody());

            }

        };
    }

    @Bean
    JpaItemListWriter<Fixture> fixtureListWriter() {

        JpaItemListWriterBuilder<Fixture> builder =  new JpaItemListWriterBuilder<>();
        builder.entityManagerFactory(entityManagerFactory);

        return builder.listWriterbuild();
    }
}