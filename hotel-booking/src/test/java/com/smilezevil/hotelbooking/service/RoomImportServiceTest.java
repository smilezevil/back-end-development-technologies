package com.smilezevil.hotelbooking.service;

import com.smilezevil.hotelbooking.batch.ImportFileResolver;
import com.smilezevil.hotelbooking.dto.ImportStartedDTO;
import com.smilezevil.hotelbooking.dto.ImportStatusDTO;
import com.smilezevil.hotelbooking.exception.ImportConflictException;
import com.smilezevil.hotelbooking.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRepository;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomImportServiceTest {

    private static final String FILE = "rooms-2027.csv";

    @Mock
    private JobLauncher jobLauncher;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private JobExplorer jobExplorer;

    @Mock
    private Job roomImportJob;

    @Mock
    private ImportFileResolver fileResolver;

    private RoomImportService service;

    @BeforeEach
    void setUp() {
        service = new RoomImportService(jobLauncher, jobRepository, jobExplorer, roomImportJob, fileResolver);
    }

    @Test
    void start_newFile_launchesJobWithFileParameter() throws Exception {
        fileExists();
        when(jobLauncher.run(eq(roomImportJob), any(JobParameters.class))).thenReturn(new JobExecution(42L));

        ImportStartedDTO started = service.start(FILE, false);

        assertThat(started).isEqualTo(new ImportStartedDTO(42L, FILE, "STARTING"));
        JobParameters parameters = launchedParameters();
        assertThat(parameters.getString("file")).isEqualTo(FILE);
        assertThat(parameters.getParameters()).doesNotContainKey("rerun");
    }

    @Test
    void start_rerun_addsParameterSoBatchCreatesNewInstance() throws Exception {
        fileExists();
        when(jobLauncher.run(eq(roomImportJob), any(JobParameters.class))).thenReturn(new JobExecution(43L));

        service.start(FILE, true);

        assertThat(launchedParameters().getLong("rerun")).isNotNull();
    }

    @Test
    void start_fileAlreadyImported_throwsConflictWithHint() throws Exception {
        fileExists();
        when(jobLauncher.run(eq(roomImportJob), any(JobParameters.class)))
                .thenThrow(new JobInstanceAlreadyCompleteException("already complete"));
        when(jobRepository.getLastJobExecution(eq("roomImportJob"), any(JobParameters.class)))
                .thenReturn(new JobExecution(7L));

        assertThatThrownBy(() -> service.start(FILE, false))
                .isInstanceOf(ImportConflictException.class)
                .hasMessage("Файл 'rooms-2027.csv' уже імпортовано (виконання 7). "
                        + "Щоб свідомо імпортувати його ще раз, додайте rerun=true");
    }

    @Test
    void start_importStillRunning_throwsConflict() throws Exception {
        fileExists();
        when(jobLauncher.run(eq(roomImportJob), any(JobParameters.class)))
                .thenThrow(new JobExecutionAlreadyRunningException("running"));

        assertThatThrownBy(() -> service.start(FILE, false))
                .isInstanceOf(ImportConflictException.class)
                .hasMessage("Імпорт файлу 'rooms-2027.csv' уже виконується");
    }

    @Test
    void start_badFileName_jobIsNotLaunched() {
        when(fileResolver.resolve("../../etc/passwd"))
                .thenThrow(new IllegalArgumentException("Недопустиме ім'я файлу"));

        assertThatThrownBy(() -> service.start("../../etc/passwd", false))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(jobLauncher);
    }

    @Test
    void status_unknownExecution_throwsNotFound() {
        when(jobExplorer.getJobExecution(999L)).thenReturn(null);

        assertThatThrownBy(() -> service.status(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Імпорту з id 999 не знайдено");
    }

    @Test
    void status_executionOfAnotherJob_throwsNotFound() {
        when(jobExplorer.getJobExecution(5L))
                .thenReturn(new JobExecution(new JobInstance(1L, "otherJob"), 5L, new JobParameters()));

        assertThatThrownBy(() -> service.status(5L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void status_finishedImport_returnsCounters() {
        JobExecution execution = new JobExecution(new JobInstance(1L, "roomImportJob"), 3L,
                new JobParametersBuilder().addString("file", FILE).toJobParameters());
        execution.setStatus(BatchStatus.COMPLETED);
        execution.setExitStatus(ExitStatus.COMPLETED);
        StepExecution step = execution.createStepExecution("roomImportStep");
        step.setReadCount(10);
        step.setWriteCount(6);
        step.setFilterCount(2);
        step.setProcessSkipCount(2);
        when(jobExplorer.getJobExecution(3L)).thenReturn(execution);

        ImportStatusDTO status = service.status(3L);

        assertThat(status.file()).isEqualTo(FILE);
        assertThat(status.status()).isEqualTo("COMPLETED");
        assertThat(status.readCount()).isEqualTo(10);
        assertThat(status.writeCount()).isEqualTo(6);
        assertThat(status.filterCount()).isEqualTo(2);
        assertThat(status.skipCount()).isEqualTo(2);
        assertThat(status.error()).isNull();
    }

    private void fileExists() {
        when(fileResolver.resolve(FILE)).thenReturn(Path.of("/data/imports", FILE));
    }

    private JobParameters launchedParameters() throws Exception {
        ArgumentCaptor<JobParameters> captor = ArgumentCaptor.forClass(JobParameters.class);
        verify(jobLauncher).run(eq(roomImportJob), captor.capture());
        return captor.getValue();
    }
}