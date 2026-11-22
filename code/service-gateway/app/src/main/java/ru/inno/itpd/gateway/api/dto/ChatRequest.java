package ru.inno.itpd.gateway.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public class ChatRequest {

    @NotBlank
    private String model;

    @NotEmpty
    @Valid
    private List<Message> messages;

    private Boolean stream = false;

    @JsonProperty("stream_options")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private StreamOptions streamOptions;

    @DecimalMin("0.0")
    @DecimalMax("2.0")
    private Double temperature;

    @JsonProperty("top_p")
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private Double topP;

    @JsonProperty("max_tokens")
    @Positive
    private Integer maxTokens;

    @JsonProperty("max_completion_tokens")
    @Positive
    private Integer maxCompletionTokens;

    private Object stop;

    public static class StreamOptions {
        @JsonProperty("include_usage")
        private Boolean includeUsage;

        public StreamOptions() {
        }

        public StreamOptions(Boolean includeUsage) {
            this.includeUsage = includeUsage;
        }

        public Boolean getIncludeUsage() { return includeUsage; }
        public void setIncludeUsage(Boolean includeUsage) { this.includeUsage = includeUsage; }
    }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public List<Message> getMessages() { return messages; }
    public void setMessages(List<Message> messages) { this.messages = messages; }
    public Boolean getStream() { return stream; }
    public void setStream(Boolean stream) { this.stream = stream; }
    public StreamOptions getStreamOptions() { return streamOptions; }
    public void setStreamOptions(StreamOptions streamOptions) { this.streamOptions = streamOptions; }
    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }
    public Double getTopP() { return topP; }
    public void setTopP(Double topP) { this.topP = topP; }
    public Integer getMaxTokens() { return maxTokens; }
    public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }
    public Integer getMaxCompletionTokens() { return maxCompletionTokens; }
    public void setMaxCompletionTokens(Integer maxCompletionTokens) { this.maxCompletionTokens = maxCompletionTokens; }
    public Object getStop() { return stop; }
    public void setStop(Object stop) { this.stop = stop; }

    @JsonIgnore
    public boolean isStreamRequested() { return Boolean.TRUE.equals(stream); }

    @Override
    public String toString() {
        return "ChatRequest(model=" + model + ", messages=" + messages + ", stream=" + stream
                + ", temperature=" + temperature + ", topP=" + topP + ", maxTokens=" + maxTokens
                + ", maxCompletionTokens=" + maxCompletionTokens + ", stop=" + stop + ")";
    }
}
