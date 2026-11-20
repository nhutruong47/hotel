package com.hsf.hotel.observability;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * P5: the {@code json-logs} profile (same logback branch as {@code prod}) writes one
 * ECS JSON object per line, and {@code /actuator/prometheus} exposes the business
 * gauges the alert rules in ops/prometheus/alerts.yml depend on.
 */
@SpringBootTest(properties = {
        "management.endpoints.web.exposure.include=health,prometheus",
        "spring.datasource.url=jdbc:h2:mem:observability;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
@ActiveProfiles({"test", "json-logs"})
@AutoConfigureMockMvc
@AutoConfigureObservability
@ExtendWith(OutputCaptureExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ObservabilityIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private RabbitTemplate rabbitTemplate;

    @Test
    void prometheusEndpointExposesBusinessGauges() throws Exception {
        String body = mockMvc.perform(get("/actuator/prometheus"))
                .andReturn().getResponse().getContentAsString();

        assertThat(body).contains("hotel_outbox_events{");
        assertThat(body).contains("status=\"pending\"");
        assertThat(body).contains("hotel_outbox_oldest_pending_age_seconds");
        assertThat(body).contains("hotel_payments_review_required");
        assertThat(body).contains("hotel_refunds_pending_manual");
        assertThat(body).contains("hotel_bookings_pending_payment");
        assertThat(body).contains("jvm_memory_used_bytes");
    }

    @Test
    void jsonLogsProfileWritesStructuredJson(CapturedOutput output) {
        // WARN: the test profile sets com.hsf.hotel to WARN.
        LoggerFactory.getLogger("com.hsf.hotel.probe").warn("json-logging-probe");

        String line = output.getOut().lines()
                .filter(l -> l.contains("json-logging-probe"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("probe log line not captured"));
        assertThat(line.trim()).startsWith("{").endsWith("}");
        assertThat(line).contains("\"@timestamp\"");
        assertThat(line).contains("\"message\":\"json-logging-probe\"");
        assertThat(line).contains("\"log.level\":\"WARN\"");
    }
}
