package gov.cms.ab2d.fetcher;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EOBFetcher {

//    @Autowired
//    BFDClient bfdClient;

    public void fetchMe(LambdaLogger logger, String correlationId,
                        long beneId, String sinceDateStr) {

        logger.log("Received fetch EOB message - corrId:" + correlationId +
                " beneId: "  + beneId + " since datestr: " + sinceDateStr + "\n");
//        logger.log("BFD Client: " + bfdClient);
    }

}
