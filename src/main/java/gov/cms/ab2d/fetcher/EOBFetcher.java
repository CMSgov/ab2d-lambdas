package gov.cms.ab2d.fetcher;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.fhir.FhirVersion;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EOBFetcher {

    @Autowired
    private BFDClient bfdClient;

    public void fetchMe(LambdaLogger logger, String correlationId,
                        long beneId, String sinceDateStr) {

        logger.log("EOB Fetcher Received fetch EOB message - corrId:" + correlationId +
                " beneId: "  + beneId + " since datestr: " + sinceDateStr + "\n");
        logger.log("BFD Client: " + bfdClient);
        IBaseBundle response = bfdClient.requestEOBFromServer(FhirVersion.R4, beneId);
        logger.log("Response: " + response);
    }

}
