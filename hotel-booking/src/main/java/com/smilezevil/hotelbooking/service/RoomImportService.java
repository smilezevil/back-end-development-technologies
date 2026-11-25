package com.smilezevil.hotelbooking.service;

import com.smilezevil.hotelbooking.batch.ImportFileResolver;
import com.smilezevil.hotelbooking.batch.RoomImportJobConfig;
import com.smilezevil.hotelbooking.dto.ImportStartedDTO;
import com.smilezevil.hotelbooking.dto.ImportStatusDTO;
import com.smilezevil.hotelbooking.exception.ImportConflictException;
import com.smilezevil.hotelbooking.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RoomImportService {

    static final String RERUN_PARAMETER = "rerun";

    private final JobLauncher jobLauncher;
    private final JobRepository jobRepository;
    private final JobExplorer jobExplorer;
    private final Job roomImportJob;
    private final ImportFileResolver fileResolver;

    @Autowired
    public RoomImportService(JobRepository jobRepository,
                             JobExplorer jobExplorer,
                             Job roomImportJob,
                             ImportFileResolver fileResolver) {
        this(asyncLauncher(jobRepository), jobRepository, jobExplorer, roomImportJob, fileResolver);
    }

    RoomImportService(JobLauncher jobLauncher,
                      JobRepository jobRepository,
                      JobExplorer jobExplorer,
                      Job roomImportJob,
                      ImportFileResolver fileResolver) {
        this.jobLauncher = jobLauncher;
        this.jobRepository = jobRepository;
        this.jobExplorer = jobExplorer;
        this.roomImportJob = roomImportJob;
        this.fileResolver = fileResolver;
    }

    public ImportStartedDTO start(String fileName, boolean rerun) {
        String file = fileResolver.resolve(fileName).getFileName().toString();
        JobParametersBuilder builder = new JobParametersBuilder()
                .addString(RoomImportJobConfig.FILE_PARAMETER, file);
        if (rerun) {
            builder.addLong(RERUN_PARAMETER, System.currentTimeMillis());
        }
        JobExecution execution = launch(file, builder.toJobParameters());
        log.info("ROOM IMPORT #{} accepted, file {}, rerun {}", execution.getId(), file, rerun);
        return new ImportStartedDTO(execution.getId(), file, execution.getStatus().name());
    }

    public ImportStatusDTO status(Long executionId) {
        JobExecution execution = jobExplorer.getJobExecution(executionId);
        if (execution == null || !RoomImportJobConfig.JOB_NAME.equals(execution.getJobInstance().getJobName())) {
            throw new ResourceNotFoundException("Імпорту з id " + executionId + " не знайдено");
        }
        return ImportStatusDTO.from(execution);
    }

    private JobExecution launch(String file, JobParameters parameters) {
        try {
            return jobLauncher.run(roomImportJob, parameters);
        } catch (JobInstanceAlreadyCompleteException e) {
            JobExecution previous = jobRepository.getLastJobExecution(RoomImportJobConfig.JOB_NAME, parameters);
            String when = previous == null ? "" : " (виконання " + previous.getId() + ")";
            throw new ImportConflictException("Файл '" + file + "' уже імпортовано" + when
                    + ". Щоб свідомо імпортувати його ще раз, додайте rerun=true");
        } catch (JobExecutionAlreadyRunningException e) {
            throw new ImportConflictException("Імпорт файлу '" + file + "' уже виконується");
        } catch (JobRestartException | JobParametersInvalidException e) {
            throw new IllegalStateException("Не вдалося запустити імпорт файлу '" + file + "': " + e.getMessage(), e);
        }
    }

    private static JobLauncher asyncLauncher(JobRepository jobRepository) {
        TaskExecutorJobLauncher launcher = new TaskExecutorJobLauncher();
        launcher.setJobRepository(jobRepository);
        launcher.setTaskExecutor(new SimpleAsyncTaskExecutor("room-import-"));
        return launcher;
    }
}