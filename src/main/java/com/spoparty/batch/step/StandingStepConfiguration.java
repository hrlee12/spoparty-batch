package com.spoparty.batch.step;


import com.spoparty.batch.Exception.ApiRequestFailedException;
import com.spoparty.batch.model.Response;
import com.spoparty.batch.entity.SeasonLeague;
import com.spoparty.batch.entity.Standings;
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
public class StandingStepConfiguration {

    private final EntityManagerFactory entityManagerFactory;
    private final FootballApiUtil footballApiUtil;

    private static final int pageSize = 1;
    private static final int chunkSize = 1;

    private final EntityParser entityParser;

    @Bean
    @JobScope
    public Step standingStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("standingStep", jobRepository)
                .<SeasonLeague, List<Standings>>chunk(chunkSize, transactionManager)
                .reader(standingSeasonLeaguejpaPagingItemReader())
                .processor(standingprocessor())
                .writer(standingListWriter())
                .faultTolerant()
                .retry(ApiRequestFailedException.class)
                .retry(DataAccessResourceFailureException.class)
                .retryLimit(3)
                .build();
    }


    @Bean
    public ItemReader<SeasonLeague> standingSeasonLeaguejpaPagingItemReader() {
        return new JpaPagingItemReaderBuilder<SeasonLeague>()
                .name("leagueJpaPagingReader")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(pageSize)
                .queryString("SELECT sl FROM SeasonLeague sl")
                .build();
    }
    @Bean
    public ItemProcessor<SeasonLeague, List<Standings>> standingprocessor() {
        return new ItemProcessor<SeasonLeague, List<Standings>>() {
            @Override
            public List<Standings> process(SeasonLeague item) throws Exception {



//                String leagueId = String.valueOf(item.getLeague().getId());
                MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
                params.add("season", "2023");
                params.add("league", String.valueOf(item.getLeague().getId()));

                ResponseEntity response = footballApiUtil.sendRequest("/standings", params, Response.class);

                if (response.getStatusCode() != HttpStatus.OK) {
                    throw new ApiRequestFailedException("API 요청에 실패하였습니다.");
                }

                return entityParser.standingParser(item, (Response)response.getBody());
            }

        };
    }

    // 리스트를 입력값으로 받으므로
    // 커스텀 writer와 builder 작성하여 사용
    @Bean
    JpaItemListWriter<Standings> standingListWriter() {

        JpaItemListWriterBuilder<Standings> builder =  new JpaItemListWriterBuilder<>();
        builder.entityManagerFactory(entityManagerFactory);

        return builder.listWriterbuild();
    }
}