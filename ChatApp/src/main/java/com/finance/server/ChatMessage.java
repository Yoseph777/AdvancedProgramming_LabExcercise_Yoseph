package com.finance.server;

import java.io.Serializable;

public class ChatMessage implements Serializable {

    public String sender;
    public String content;
    public long timestamp;

    public ChatMessage(
            String sender,
            String content
    ) {
        this.sender = sender;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return sender + ": " + content;
    }
}