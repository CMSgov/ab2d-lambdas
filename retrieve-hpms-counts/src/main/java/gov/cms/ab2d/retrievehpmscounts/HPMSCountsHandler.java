package gov.cms.ab2d.retrievehpmscounts;

import com.amazonaws.SDKGlobalConfiguration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import com.amazonaws.util.StringUtils;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import gov.cms.ab2d.contracts.model.ContractDTO;
import gov.cms.ab2d.eventclient.config.Ab2dEnvironment;
import gov.cms.ab2d.lambdalibs.lib.PropertiesUtil;
import gov.cms.ab2d.snsclient.clients.SNSClient;
import gov.cms.ab2d.snsclient.clients.SNSConfig;
import gov.cms.ab2d.snsclient.messages.CoverageCountDTO;
import lombok.SneakyThrows;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.BasicHttpClientResponseHandler;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.joda.time.DateTime;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalField;
import java.util.*;
import java.util.stream.Collectors;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES;
import static gov.cms.ab2d.snsclient.messages.AB2DServices.HPMS;
import static gov.cms.ab2d.snsclient.messages.Topics.COVERAGE_COUNTS;


public class HPMSCountsHandler implements RequestStreamHandler {

    private final ObjectMapper mapperIn = JsonMapper.builder()
            .configure(ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
            .configure(FAIL_ON_UNKNOWN_PROPERTIES, true)
            .build()
            .registerModule(new ParameterNamesModule(JsonCreator.Mode.PROPERTIES))
            .registerModule(new JavaTimeModule())
            .registerModule(new JodaModule());

    private final ObjectMapper mapperOut = JsonMapper.builder()
            .configure(ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
            .configure(FAIL_ON_UNKNOWN_PROPERTIES, true)
            .build()
            .registerModule(new ParameterNamesModule(JsonCreator.Mode.PROPERTIES));


    private final CloseableHttpClient httpClient;
    private final AmazonSNSClient snsClient;

    public HPMSCountsHandler() {
        httpClient = HttpClients.createDefault();
        if (!StringUtils.isNullOrEmpty(System.getenv("IS_LOCALSTACK"))) {
            System.setProperty(SDKGlobalConfiguration.DISABLE_CERT_CHECKING_SYSTEM_PROPERTY, "true");
            this.snsClient = (AmazonSNSClient) AmazonSNSClientBuilder.standard()
                    .withEndpointConfiguration(new AwsClientBuilder
                            .EndpointConfiguration("https://localhost:4566",
                            Regions.US_EAST_1.getName()))
                    .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials("access_key_id", "secret_key_id")))
                    .build();
        } else {
            this.snsClient = (AmazonSNSClient) AmazonSNSClientBuilder.standard().build();
        }
    }

    /*
     * Test constructor to inject clients*/
    public HPMSCountsHandler(CloseableHttpClient httpClient, AmazonSNSClient snsClient) {
        this.httpClient = httpClient;
        this.snsClient = snsClient;
    }

    @SneakyThrows
    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) {
        Properties prop = PropertiesUtil.loadProps();
        String url = prop.get("contract_service_url") + "";
        String envi = Optional.ofNullable(prop.get("environment")).orElse("local") + "";
        HttpGet request = new HttpGet(url + "/contracts");
        HttpClientResponseHandler<String> handler = new BasicHttpClientResponseHandler();
        String response = httpClient.execute(request, handler);
        SNSConfig snsConfig = new SNSConfig();
        SNSClient client = snsConfig.snsClient(snsClient, Ab2dEnvironment.fromName(envi));
        DateTime dateTime = DateTime.now();
        Timestamp version = Timestamp.from(Instant.now());
        int year = dateTime.getYear();
        int month = dateTime.getMonthOfYear();
        String hpms = HPMS.toString();
        List<CoverageCountDTO> coverage =
                Arrays.stream(mapperIn.readValue(response, ContractDTO[].class))
                        .map(contract -> new CoverageCountDTO(contract.getContractNumber(), hpms, contract.getMedicareEligible(), year, month, version))
                        .collect(Collectors.toList());
        client.sendMessage(COVERAGE_COUNTS.getValue(), coverage);
        outputStream.write(("{\"status\": \"ok\"}").getBytes(StandardCharsets.UTF_8));
    }

}
