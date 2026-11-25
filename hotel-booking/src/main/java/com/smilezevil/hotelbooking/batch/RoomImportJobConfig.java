package com.smilezevil.hotelbooking.batch;

import com.smilezevil.hotelbooking.entity.Room;
import com.smilezevil.hotelbooking.repository.HotelRepository;
import com.smilezevil.hotelbooking.repository.RoomRepository;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.data.builder.RepositoryItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

import java.nio.charset.StandardCharsets;

@Configuration
@EnableConfigurationProperties(RoomImportProperties.class)
public class RoomImportJobConfig {

    public static final String JOB_NAME = "roomImportJob";
    public static final String FILE_PARAMETER = "file";

    @Bean
    @StepScope
    public FlatFileItemReader<RoomCsvRow> roomCsvReader(@Value("#{jobParameters['file']}") String fileName,
                                                        ImportFileResolver fileResolver) {
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setNames(RoomCsvRow.COLUMNS);

        FlatFileItemReader<RoomCsvRow> reader = new FlatFileItemReader<>();
        reader.setName("roomCsvReader");
        reader.setResource(new FileSystemResource(fileResolver.resolve(fileName)));
        reader.setEncoding(StandardCharsets.UTF_8.name());
        reader.setLinesToSkip(1);
        reader.setLineMapper((line, lineNumber) -> RoomCsvRow.from(lineNumber, tokenizer.tokenize(line)));
        return reader;
    }

    @Bean
    @StepScope
    public RoomImportProcessor roomImportProcessor(RoomRepository roomRepository, HotelRepository hotelRepository) {
        return new RoomImportProcessor(roomRepository, hotelRepository);
    }

    @Bean
    public RepositoryItemWriter<Room> roomWriter(RoomRepository roomRepository) {
        return new RepositoryItemWriterBuilder<Room>()
                .repository(roomRepository)
                .build();
    }

    @Bean
    public Step roomImportStep(JobRepository jobRepository,
                               PlatformTransactionManager transactionManager,
                               RoomImportProperties properties,
                               FlatFileItemReader<RoomCsvRow> roomCsvReader,
                               RoomImportProcessor roomImportProcessor,
                               RepositoryItemWriter<Room> roomWriter) {
        return new StepBuilder("roomImportStep", jobRepository)
                .<RoomCsvRow, Room>chunk(properties.chunkSize(), transactionManager)
                .reader(roomCsvReader)
                .processor(roomImportProcessor)
                .writer(roomWriter)
                .faultTolerant()
                .skip(InvalidRoomRowException.class)
                .skip(FlatFileParseException.class)
                .skipLimit(properties.skipLimit())
                .listener(new RoomImportSkipListener())
                .build();
    }

    @Bean
    public Job roomImportJob(JobRepository jobRepository, Step roomImportStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .listener(new ImportIdentityListener())
                .start(roomImportStep)
                .build();
    }
}