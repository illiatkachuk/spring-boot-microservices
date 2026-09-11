package travel.winwin.authapi.service;

import java.util.UUID;
import org.springframework.stereotype.Service;
import travel.winwin.authapi.client.DataApiClient;
import travel.winwin.authapi.entity.ProcessingLog;
import travel.winwin.authapi.repository.ProcessingLogRepository;

@Service
public class ProcessService {

    private final DataApiClient dataApiClient;
    private final ProcessingLogRepository processingLogRepository;

    public ProcessService(DataApiClient dataApiClient, ProcessingLogRepository processingLogRepository) {
        this.dataApiClient = dataApiClient;
        this.processingLogRepository = processingLogRepository;
    }

    /**
     * Sends the text to data-api, records input and output in processing_log, and returns the output.
     *
     * <p>If data-api fails, the exception stops us before anything is saved. The method is intentionally
     * not @Transactional: a DB transaction shouldn't stay open while we wait on a network call.
     */
    public String process(UUID userId, String text) {
        String result = dataApiClient.transform(text);
        processingLogRepository.save(new ProcessingLog(userId, text, result));
        return result;
    }
}
