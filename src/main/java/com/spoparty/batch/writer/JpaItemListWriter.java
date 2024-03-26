package com.spoparty.batch.writer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.beans.factory.InitializingBean;

import java.util.ArrayList;
import java.util.List;


// 기본 writer는 리스트를 입력값으로 받지 않음.
// writer로 리스트를 처리할 수 있게 jpaItemWrtier<T>를 상속하여 커스텀 writer 만들기
@Slf4j
//@RequiredArgsConstructor
public class JpaItemListWriter<T> extends JpaItemWriter<List<T>> {
    private JpaItemWriter<T> jpaItemWriter;

    public JpaItemListWriter(JpaItemWriter<T> writer) {
        this.jpaItemWriter = writer;
    }

    @Override
    public void write(Chunk<? extends List<T>> items)  {
        Chunk<T> flattenChunk = new Chunk<>();

        for (List<T> list : items) {
            flattenChunk.addAll(list);
        }

        jpaItemWriter.write(flattenChunk);
    }
}