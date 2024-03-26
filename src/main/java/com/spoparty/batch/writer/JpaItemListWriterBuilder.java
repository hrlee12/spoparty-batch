package com.spoparty.batch.writer;

import com.spoparty.batch.entity.SeasonLeagueTeamPlayer;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.util.Assert;

import java.util.List;

// 커스텀한 writer를 처리할 커스텀 writerBuilder 작성.
public class JpaItemListWriterBuilder<T> extends JpaItemWriterBuilder<T> {
    private EntityManagerFactory entityManagerFactory;
    private boolean usePersist = false;


    public JpaItemWriterBuilder<T> entityManagerFactory(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
        return this;
    }

    public JpaItemWriterBuilder<T> usePersist(boolean usePersist) {
        this.usePersist = usePersist;
        return this;
    }

    public JpaItemListWriter<T> listWriterbuild() {
        Assert.state(this.entityManagerFactory != null, "EntityManagerFactory must be provided");
        JpaItemWriter<SeasonLeagueTeamPlayer> jpaItemWriter =  new JpaItemWriterBuilder<SeasonLeagueTeamPlayer>()
                .entityManagerFactory(entityManagerFactory)
                .build();

        JpaItemListWriter<T> writer = new JpaItemListWriter(jpaItemWriter);
        writer.setEntityManagerFactory(this.entityManagerFactory);
        writer.setUsePersist(this.usePersist);
        return writer;
    }
}
