package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.eventclient.clients.EventClient;
import gov.cms.ab2d.eventclient.events.LoggableEvent;
import org.springframework.stereotype.Service;

@Service
public class MockEventClient implements EventClient {
    @Override
    public void sendLogs(LoggableEvent loggableEvent) {
        // TDDO - get fancier with mocking at a later point.
    }
}
