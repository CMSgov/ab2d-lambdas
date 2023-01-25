package gov.cms.ab2d.coveragecounts;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import gov.cms.ab2d.databasemanagement.DatabaseUtil;
import lombok.SneakyThrows;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES;


public class CoverageCountsHandler implements RequestStreamHandler {

    private final ObjectMapper mapper = JsonMapper.builder()
            .configure(ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
            .configure(FAIL_ON_UNKNOWN_PROPERTIES, true)
            .build()
            .registerModule(new ParameterNamesModule(JsonCreator.Mode.PROPERTIES))
            .registerModule(new JodaModule());


    @SneakyThrows
    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) {
        String eventString = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        context.getLogger()
                .log(eventString);
        SNSEvent event = mapper.readValue(eventString, SNSEvent.class);
        context.getLogger()
                .log(event.toString());
        Connection connection = DatabaseUtil.getConnection();
        PreparedStatement stmt = connection.prepareStatement("INSERT INTO lambda.coverage_counts\n" +
                "(CONTRACT_NUMBER, SERVICE, COUNT, YEAR, MONTH, CREATE_AT, COUNTED_AT) \n" +
                "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, ?)");
        context.getLogger()
                .log(String.valueOf(event.getRecords()));
        Optional.ofNullable(event.getRecords())
                .orElse(new ArrayList<>())
                .forEach(record -> {
                    context.getLogger()
                            .log("checking records");
                    try {
                        Arrays.asList(mapper.readValue(record.getSNS()
                                        .getMessage(), CoverageCountDTO[].class))
                                .forEach(count -> {
                                    context.getLogger()
                                            .log("populating obj");
                                    context.getLogger()
                                            .log(count.getContractNumber());
                                    try {
                                        stmt.setString(1, count.getContractNumber());
                                        stmt.setString(2, count.getService());
                                        stmt.setInt(3, count.getCount());
                                        stmt.setInt(4, count.getYear());
                                        stmt.setInt(5, count.getMonth());
                                        stmt.setTimestamp(6, count.getCountedAt());
                                        stmt.addBatch();
                                    } catch (SQLException e) {
                                        context.getLogger()
                                                .log(e.getMessage());
                                        throw new RuntimeException(e);
                                    }
                                });
                    } catch (Exception e) {
                        context.getLogger()
                                .log(e.getMessage());
                        throw new RuntimeException(e);
                    }
                });

        int[] id = stmt.executeBatch();

        outputStream.write(("{\"status\": \"ok\", \"Updated\":\"" + Arrays.toString(id) + "\" }").getBytes(StandardCharsets.UTF_8));
    }
}
