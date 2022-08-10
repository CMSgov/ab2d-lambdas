package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.eventclient.clients.EventClient;
import gov.cms.ab2d.eventclient.events.LoggableEvent;

public class MockEventClient implements EventClient {
    @Override
    public void send(LoggableEvent loggableEvent) {
        // TDDO - get fancier with mocking at a later point.
    }
}
