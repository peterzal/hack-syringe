package com.sailthru.deliverability.core;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.QueueDoesNotExistException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sailthru.deliverability.api.BounceIncomingMessage;
import com.sailthru.deliverability.api.BounceIncomingMessageResults;
import com.sailthru.deliverability.api.BounceOutgoingMessage;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.AssertionErrors.assertEquals;

@ActiveProfiles("MessageProcessor-test")
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
// Not fully clear how this config works. TODO: figure out if it can be removed and simplified
@ContextConfiguration(classes = MessageProcessorTestConfiguration.class)
public class MessageProcessorTest {

    @Rule public ExpectedException thrown = ExpectedException.none();

    @Mock private AmazonSQS mockAmazonSQS;
    @Mock private GetQueueUrlResult mockQueueUrlResult;
    @Mock private MeterRegistry meterRegistry;
    @Mock private Counter successes;
    @Mock private Counter failures;

    private String sqsQueueUrl = "http://mock-queue:1234/queue/foo";
    private BounceClassifier bounceClassifier;
    private MessageProcessor messageProcessor;

    @Before
    public void setUp() throws Exception {
        when(mockAmazonSQS.getQueueUrl(anyString())).thenReturn(mockQueueUrlResult);
        when(mockQueueUrlResult.getQueueUrl()).thenReturn(sqsQueueUrl);
        when(meterRegistry.counter(eq("messages_total"), eq("result"), eq("success"))).thenReturn(successes);
        when(meterRegistry.counter(eq("messages_total"), eq("result"), eq("failure"))).thenReturn(failures);

        bounceClassifier = new BounceClassifier("bounce_rules_valid.txt");
        messageProcessor = new MessageProcessor(mockAmazonSQS, bounceClassifier, meterRegistry);

        ReflectionTestUtils.setField(messageProcessor, "sqsQueueName", "http://mock-queue:1234/queue/foo");
        messageProcessor.init();
    }

    @Test
    public void testReceive() throws Exception {
        BounceIncomingMessage incomingMessage = createBounceMessage();
        messageProcessor.receive(incomingMessage);

        BounceOutgoingMessage bounceOutgoingMessage = bounceClassifier.buildBounceJSON(incomingMessage);

        verify(mockAmazonSQS).getQueueUrl(eq(sqsQueueUrl));
        verify(mockAmazonSQS).sendMessage(eq(sqsQueueUrl), eq(bounceOutgoingMessage.toJsonString()));
        verify(successes).increment();
        verifyZeroInteractions(failures);
    }

    @Test
    public void testReceiveRetriesOnceThenSucceeds() throws Exception {
        BounceIncomingMessage incomingMessage = createBounceMessage();
        BounceOutgoingMessage bounceOutgoingMessage = bounceClassifier.buildBounceJSON(incomingMessage);

        // First throw an exception, then return null is fine to indicate success (because the value isn't checked)
        when(mockAmazonSQS.sendMessage(eq(sqsQueueUrl), eq(bounceOutgoingMessage.toJsonString())))
            .thenThrow(QueueDoesNotExistException.class).thenReturn(null);

        messageProcessor.receive(incomingMessage);

        verify(mockAmazonSQS).getQueueUrl(eq(sqsQueueUrl));
        verify(mockAmazonSQS, times(MessageProcessor.MAX_PROCESS_RETIRES)).sendMessage(eq(sqsQueueUrl), eq(bounceOutgoingMessage.toJsonString()));
        verify(successes).increment();
        verify(failures).increment();
    }

    @Test
    public void testReceiveRetriesAndFails() throws Exception {
        BounceIncomingMessage incomingMessage = createBounceMessage();
        BounceOutgoingMessage bounceOutgoingMessage = bounceClassifier.buildBounceJSON(incomingMessage);

        // First throw an exception, then return null is fine to indicate success (because the return value isn't checked)
        when(mockAmazonSQS.sendMessage(eq(sqsQueueUrl), eq(bounceOutgoingMessage.toJsonString())))
            .thenThrow(QueueDoesNotExistException.class);

        messageProcessor.receive(incomingMessage);

        verify(mockAmazonSQS).getQueueUrl(eq(sqsQueueUrl));
        verify(mockAmazonSQS, times(MessageProcessor.MAX_PROCESS_RETIRES)).sendMessage(eq(sqsQueueUrl), eq(bounceOutgoingMessage.toJsonString()));
        verifyZeroInteractions(successes);
        verify(failures, times(MessageProcessor.MAX_PROCESS_RETIRES)).increment();
    }

    @Test
    public void testReceiveWithMultipleResultsUsesFinal() throws Exception {
        long lastCode = 100L;
        String lastDesc = "blep";
        BounceIncomingMessage incomingMessage = createBounceMessageWithMulitpleResults(lastCode, lastDesc);
        messageProcessor.receive(incomingMessage);

        verify(mockAmazonSQS).getQueueUrl(eq(sqsQueueUrl));
        verify(successes).increment();
        verifyZeroInteractions(failures);

        ArgumentCaptor<String> bounceOutgoingMessageJsonStringCapture = ArgumentCaptor.forClass(String.class);
        verify(mockAmazonSQS).sendMessage(eq(sqsQueueUrl), bounceOutgoingMessageJsonStringCapture.capture());

        List<String> bounceOutgoingStrings = bounceOutgoingMessageJsonStringCapture.getAllValues();
        BounceOutgoingMessage bounceOutgoingMessage = new ObjectMapper().readValue(bounceOutgoingStrings.get(0), BounceOutgoingMessage.class);
        String expectedLogField = lastCode + "  " + lastDesc;
        assertEquals("BounceOutgoingMessage \"logField\" was not chosen correctly", expectedLogField, bounceOutgoingMessage.getLog());
    }

    @Test
    public void testInvalidBounceWhenEmptyResults() {
        BounceIncomingMessage invalidMessage = createInvalidBounceMessage();
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Bounce incoming message \"results\" field cannot be empty");
        bounceClassifier.buildBounceJSON(invalidMessage);
    }

    private BounceIncomingMessage createBounceMessage() {
        BounceIncomingMessageResults bounceIncomingMessageResults
            = new BounceIncomingMessageResults("baz", "qux", 99L, "1/1/2001", "corge", "grault");
        List<BounceIncomingMessageResults> bounceIncomingMessageResultsList = Collections.singletonList(bounceIncomingMessageResults);
        return new BounceIncomingMessage("foo", "bar", "123", bounceIncomingMessageResultsList);
    }

    private BounceIncomingMessage createInvalidBounceMessage() {
        return new BounceIncomingMessage("foo", "bar", "123", Collections.emptyList());
    }

    private BounceIncomingMessage createBounceMessageWithMulitpleResults(long lastCode, String lastDesc) {
        BounceIncomingMessageResults bounceIncomingMessageResults0
            = new BounceIncomingMessageResults("baz", "qux", 99L, "1/1/2001", "corge", "grault");
        BounceIncomingMessageResults bounceIncomingMessageResults1
            = new BounceIncomingMessageResults(lastDesc, "qux", lastCode, "1/1/2001", "corge", "grault");

        // "results" sub JSON is invalid if JSON array size != 1

        List<BounceIncomingMessageResults> bounceIncomingMessageResultsList =  Lists.newArrayList(bounceIncomingMessageResults0,
            bounceIncomingMessageResults1);

        return new BounceIncomingMessage("foo", "bar", "123", bounceIncomingMessageResultsList);
    }
}
