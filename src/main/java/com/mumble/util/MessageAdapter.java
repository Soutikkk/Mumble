package com.mumble.util;

import com.google.gson.*;
import com.mumble.model.Message;
import com.mumble.model.SystemMessage;
import com.mumble.model.TextMessage;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Custom Gson adapter to handle polymorphism of Message class.
 */
public class MessageAdapter implements JsonSerializer<Message>, JsonDeserializer<Message> {

    @Override
    public JsonElement serialize(Message src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject result = new JsonObject();
        result.addProperty("type", src.getType());
        result.addProperty("sender", src.getSender());
        result.addProperty("timestamp", src.getTimestamp().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        if (src instanceof TextMessage) {
            TextMessage tm = (TextMessage) src;
            result.addProperty("content", tm.getContent());
            result.addProperty("receiver", tm.getReceiver());
            result.addProperty("id", tm.getId());
            result.addProperty("replyToMessageId", tm.getReplyToMessageId());
            result.addProperty("isDelivered", tm.isDelivered());
            result.addProperty("isRead", tm.isRead());
            result.addProperty("isDeleted", tm.isDeleted());
        } else if (src instanceof SystemMessage) {
            SystemMessage sm = (SystemMessage) src;
            result.addProperty("systemAction", sm.getSystemAction());
            result.addProperty("systemContent", sm.getSystemContent());
            result.addProperty("targetMessageId", sm.getTargetMessageId());
            result.addProperty("targetUsername", sm.getTargetUsername());
        }
        return result;
    }

    @Override
    public Message deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        String type = jsonObject.get("type").getAsString();
        String sender = jsonObject.get("sender").getAsString();
        String timestampStr = jsonObject.get("timestamp").getAsString();
        LocalDateTime timestamp = LocalDateTime.parse(timestampStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        Message message;
        if ("TEXT".equals(type)) {
            String receiver = jsonObject.has("receiver") && !jsonObject.get("receiver").isJsonNull() ? jsonObject.get("receiver").getAsString() : null;
            String content = jsonObject.has("content") && !jsonObject.get("content").isJsonNull() ? jsonObject.get("content").getAsString() : null;
            TextMessage tm = new TextMessage(sender, receiver, content);
            
            if (jsonObject.has("id") && !jsonObject.get("id").isJsonNull()) {
                tm.setId(jsonObject.get("id").getAsString());
            }
            tm.setTimestamp(timestamp);
            if (jsonObject.has("replyToMessageId") && !jsonObject.get("replyToMessageId").isJsonNull()) {
                tm.setReplyToMessageId(jsonObject.get("replyToMessageId").getAsString());
            }
            if (jsonObject.has("isDelivered")) {
                tm.setDelivered(jsonObject.get("isDelivered").getAsBoolean());
            }
            if (jsonObject.has("isRead")) {
                tm.setRead(jsonObject.get("isRead").getAsBoolean());
            }
            if (jsonObject.has("isDeleted")) {
                tm.setDeleted(jsonObject.get("isDeleted").getAsBoolean());
            }
            message = tm;
        } else {
            String systemAction = jsonObject.get("systemAction").getAsString();
            String systemContent = jsonObject.get("systemContent").getAsString();
            SystemMessage sm = new SystemMessage(sender, systemAction, systemContent);
            sm.setTimestamp(timestamp);
            if (jsonObject.has("targetMessageId") && !jsonObject.get("targetMessageId").isJsonNull()) {
                sm.setTargetMessageId(jsonObject.get("targetMessageId").getAsString());
            }
            if (jsonObject.has("targetUsername") && !jsonObject.get("targetUsername").isJsonNull()) {
                sm.setTargetUsername(jsonObject.get("targetUsername").getAsString());
            }
            message = sm;
        }
        return message;
    }
}
