package com.sailthru.deliverability.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import junit.framework.TestCase;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;

public class BounceOutgoingMessageTest extends TestCase {

    @Test
    public void testOutgoingMessage() throws JsonProcessingException, IOException {
        String expectedOutput = "{\"timestamp\":\"2018-07-16 23:44:04\",\"to_list\":\"tes5552@paperdollblanket.com\",\"msgid\":\"20180716194404.13884792.11\",\"log\":\"550  <tes5552@paperdollblanket.com>: Recipient address rejected: User unknown in virtual mailbox table\",\"status\":\"hardbounce\"}";
        String log = "550  <tes5552@paperdollblanket.com>: Recipient address rejected: User unknown in virtual mailbox table";
        String status = "HARDBOUNCE";
        String msgid = "20180716194404.13884792.11";
        String toList = "tes5552@paperdollblanket.com";
        String timestamp = "2018-07-16 23:44:04";
        BounceOutgoingMessage msg = BounceOutgoingMessage
            .builder()
            .log(log)
            .msgid(msgid)
            .status(status)
            .timestamp(timestamp)
            .toList(toList)
            .build();
        ObjectMapper mapper = new ObjectMapper();

        String jsonString = mapper.writeValueAsString(msg);

        assertEquals(expectedOutput, jsonString);
    }
}