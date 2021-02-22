// Copyright 2020 The NATS Authors
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at:
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package io.nats.client;

import io.nats.client.impl.JsonUtils;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

import static io.nats.client.support.ApiConstants.*;

/**
 * The ConsumerConfiguration class specifies the configuration for creating a JetStream consumer on the client and
 * if necessary the server.
 * Options are created using a {@link PublishOptions.Builder Builder}.
 */
public class ConsumerConfiguration {

    /**
     * The delivery policy for this consumer.
     */
    public enum DeliverPolicy {
        All("all"),
        Last("last"),
        New("new"),
        ByStartSequence("by_start_sequence"),
        ByStartTime("by_start_time");

        private String policy;

        DeliverPolicy(String p) {
            policy = p;
        }

        @Override
        public String toString() {
            return policy;
        }
        
        private static final Map<String, DeliverPolicy> strEnumHash = new HashMap<>();
        static {
            for(DeliverPolicy env : DeliverPolicy.values()) {
                strEnumHash.put(env.toString(), env);
            }
        }

        public static DeliverPolicy get(String value) {
            return strEnumHash.get(value);
        }
    }

    /**
      * Represents the Ack Policy of a consumer
     */  
    public enum AckPolicy {
        None("none"),
        All("all"),
        Explicit("explicit");

        private String policy;

        AckPolicy(String p) {
            policy = p;
        }

        @Override
        public String toString() {
            return policy;
        }

        private static final Map<String, AckPolicy> strEnumHash = new HashMap<>();
        static {
            for(AckPolicy env : AckPolicy.values()) {
                strEnumHash.put(env.toString(), env);
            }
        }

        public static AckPolicy get(String value) {
            return strEnumHash.get(value);
        }
    }

    /**
     * Represents the replay policy of a consumer.
     */
    public enum ReplayPolicy {
        Instant("instant"),
        Original("original");

        private String policy;

        ReplayPolicy(String p) {
            this.policy = p;
        }

        @Override
        public String toString() {
            return policy;
        }

        private static final Map<String, ReplayPolicy> strEnumHash = new HashMap<>();
        static {
            for(ReplayPolicy env : ReplayPolicy.values()) {
                strEnumHash.put(env.toString(), env);
            }
        }

        public static ReplayPolicy get(String value) {
            return strEnumHash.get(value);
        }        
    }

    private final DeliverPolicy deliverPolicy;
    private final AckPolicy ackPolicy;
    private final ReplayPolicy replayPolicy;

    private String durable;
    private String deliverSubject;
    private final long startSeq;
    private final ZonedDateTime startTime;
    private final Duration ackWait;
    private final long maxDeliver;
    private String filterSubject;
    private final String sampleFrequency;
    private final long rateLimit;
    private long maxAckPending;

    /**
     * Sets the durable name of the configuration.
     * @param value name of the durable
     */
    void setDurable(String value) {
        durable = value;
    }

    /**
     * Package level API to set the deliver subject in the creation API.
     * @param subject - Subject to deliver messages.
     */
    void setDeliverSubject(String subject) {
        this.deliverSubject = subject;
    }

    /**
     * Sets the filter subject of the configuration.
     * @param subject filter subject.
     */
    void setFilterSubject(String subject) {
        this.filterSubject = subject;
    }

    /**
     * Sets the maximum ack pending.
     * @param maxAckPending maximum pending acknowledgements.
     */
    void setMaxAckPending(long maxAckPending) {
        this.maxAckPending = maxAckPending;
    }

    // for the response from the server
    ConsumerConfiguration(String json) {

        Matcher m = DELIVER_POLICY_RE.matcher(json);
        deliverPolicy = m.find() ? DeliverPolicy.get(m.group(1)) : DeliverPolicy.All;

        m = ACK_POLICY_RE.matcher(json);
        ackPolicy = m.find() ? AckPolicy.get(m.group(1)) : AckPolicy.Explicit;

        m = REPLAY_POLICY_RE.matcher(json);
        replayPolicy = m.find() ? ReplayPolicy.get(m.group(1)) : ReplayPolicy.Instant;

        durable = JsonUtils.readString(json, DURABLE_NAME_RE);
        deliverSubject = JsonUtils.readString(json, DELIVER_SUBJECT_RE);
        startSeq = JsonUtils.readLong(json, OPT_START_SEQ_RE, 0);
        startTime = JsonUtils.readDate(json, OPT_START_TIME_RE);
        ackWait = JsonUtils.readDuration(json, ACK_WAIT_RE, Duration.ofSeconds(30));
        maxDeliver = JsonUtils.readLong(json, MAX_DELIVER_RE, -1);
        filterSubject = JsonUtils.readString(json, FILTER_SUBJECT_RE);
        sampleFrequency = JsonUtils.readString(json, SAMPLE_FREQ_RE);
        rateLimit = JsonUtils.readLong(json, RATE_LIMIT_RE, 0);
        maxAckPending = JsonUtils.readLong(json, MAX_ACK_PENDING_RE, 0);
    }

    // For the builder
    ConsumerConfiguration(String durable, DeliverPolicy deliverPolicy, long startSeq,
            ZonedDateTime startTime, AckPolicy ackPolicy, Duration ackWait, long maxDeliver, String filterSubject,
            ReplayPolicy replayPolicy, String sampleFrequency, long rateLimit, String deliverSubject, long maxAckPending) {
                this.durable = durable;
                this.deliverPolicy = deliverPolicy;
                this.startSeq = startSeq;
                this.startTime = startTime;
                this.ackPolicy = ackPolicy;
                this.ackWait = ackWait;
                this.maxDeliver = maxDeliver;
                this.filterSubject = filterSubject;
                this.replayPolicy = replayPolicy;
                this.sampleFrequency = sampleFrequency;
                this.rateLimit = rateLimit;
                this.deliverSubject = deliverSubject;
                this.maxAckPending = maxAckPending;
    }

    /**
     * Returns a JSON representation of this consumer configuration.
     * 
     * @param streamName name of the stream.
     * @return json consumer configuration to send to the server.
     */
    public String toJSON(String streamName) {
        
        StringBuilder sb = new StringBuilder("{");
        
        JsonUtils.addFld(sb, STREAM_NAME, streamName);
        
        sb.append("\"config\" : {");
        
        JsonUtils.addFld(sb, DURABLE_NAME, durable);
        JsonUtils.addFld(sb, DELIVER_SUBJECT, deliverSubject);
        JsonUtils.addFld(sb, DELIVER_POLICY, deliverPolicy.toString());
        JsonUtils.addFld(sb, OPT_START_SEQ, startSeq);
        JsonUtils.addFld(sb, OPT_START_TIME, startTime);
        JsonUtils.addFld(sb, ACK_POLICY, ackPolicy.toString());
        JsonUtils.addFld(sb, ACK_WAIT, ackWait);
        JsonUtils.addFld(sb, MAX_DELIVER, maxDeliver);
        JsonUtils.addFld(sb, MAX_ACK_PENDING, maxAckPending);
        JsonUtils.addFld(sb, FILTER_SUBJECT, filterSubject);
        JsonUtils.addFld(sb, REPLAY_POLICY, replayPolicy.toString());
        JsonUtils.addFld(sb, SAMPLE_FREQ, sampleFrequency);
        JsonUtils.addFld(sb, RATE_LIMIT, rateLimit);

        // remove the trailing ','
        sb.setLength(sb.length()-1);
        sb.append("}}");

        return sb.toString();
    }

    /**
     * Gets the name of the durable subscription for this consumer configuration.
     * @return name of the durable.
     */
    public String getDurable() {
        return durable;
    }

    /**
     * Gets the deliver subject of this consumer configuration.
     * @return the deliver subject.
     */    
    public String getDeliverSubject() {
        return deliverSubject;
    }

    /**
     * Gets the deliver policy of this consumer configuration.
     * @return the deliver policy.
     */    
    public DeliverPolicy getDeliverPolicy() {
        return deliverPolicy;
    }

    /**
     * Gets the start sequence of this consumer configuration.
     * @return the start sequence.
     */    
    public long getStartSequence() {
        return startSeq;
    }

    /**
     * Gets the start time of this consumer configuration.
     * @return the start time.
     */    
    public ZonedDateTime getStartTime() {
        return startTime;
    }

    /**
     * Gets the acknowledgment policy of this consumer configuration.
     * @return the acknowledgment policy.
     */    
    public AckPolicy getAckPolicy() {
        return ackPolicy;
    }

    /**
     * Gets the acknowledgment wait of this consumer configuration.
     * @return the acknowledgment wait duration.
     */     
    public Duration getAckWait() {
        return ackWait;
    }

    /**
     * Gets the max delivery amount of this consumer configuration.
     * @return the max delivery amount.
     */      
    public long getMaxDeliver() {
        return maxDeliver;
    }

    /**
     * Gets the max filter subject of this consumer configuration.
     * @return the filter subject.
     */    
    public String getFilterSubject() {
        return filterSubject;
    }

    /**
     * Gets the replay policy of this consumer configuration.
     * @return the replay policy.
     */     
    public ReplayPolicy getReplayPolicy() {
        return replayPolicy;
    }

    /**
     * Gets the rate limit for this consumer configuration.
     * @return the rate limit in msgs per second.
     */      
    public long getRateLimit() {
        return rateLimit;
    }

    /**
     * Gets the maximum ack pending configuration.
     * @return maxumum ack pending.
     */
    public long getMaxAckPending() {
        return maxAckPending;
    }

    /**
     * Creates a builder for the publish options.
     * @return a publish options builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a builder for the publish options.
     * @param cc the consumer configuration
     * @return a publish options builder
     */
    public static Builder builder(ConsumerConfiguration cc) {
        return cc == null ? new Builder() : new Builder(cc);
    }

    /**
     * ConsumerConfiguration is created using a Builder. The builder supports chaining and will
     * create a default set of options if no methods are calls.
     * 
     * <p>{@code new ConsumerConfiguration.Builder().build()} will create a default ConsumerConfiguration.
     * 
     */
    public static class Builder {

        private String durable = null;
        private DeliverPolicy deliverPolicy = DeliverPolicy.All;
        private long startSeq = 0;
        private ZonedDateTime startTime = null;
        private AckPolicy ackPolicy = AckPolicy.Explicit;
        private Duration ackWait = Duration.ofSeconds(30);
        private long maxDeliver = -1;
        private String filterSubject = null;
        private ReplayPolicy replayPolicy = ReplayPolicy.Instant;
        private String sampleFrequency = null;
        private long rateLimit = 0;
        private String deliverSubject = null;
        private long maxAckPending = 0;

        public String getDurable() {
            return durable;
        }

        public String getDeliverSubject() {
            return deliverSubject;
        }

        public long getMaxAckPending() {
            return maxAckPending;
        }

        public Builder() {}

        public Builder(ConsumerConfiguration cc) {
            this.durable = cc.durable;
            this.deliverPolicy = cc.deliverPolicy;
            this.startSeq = cc.startSeq;
            this.startTime = cc.startTime;
            this.ackPolicy = cc.ackPolicy;
            this.ackWait = cc.ackWait;
            this.maxDeliver = cc.maxDeliver;
            this.filterSubject = cc.filterSubject;
            this.replayPolicy = cc.replayPolicy;
            this.sampleFrequency = cc.sampleFrequency;
            this.rateLimit = cc.rateLimit;
            this.deliverSubject = cc.deliverSubject;
            this.maxAckPending = cc.maxAckPending;
        }

        /**
         * Sets the name of the durable subscription.
         * @param durable name of the durable subscription.
         * @return the builder
         */
        public Builder durable(String durable) {
            this.durable = durable;
            return this;
        }      

        /**
         * Sets the delivery policy of the ConsumerConfiguration.
         * @param policy the delivery policy.
         * @return Builder
         */
        public Builder deliverPolicy(DeliverPolicy policy) {
            this.deliverPolicy = policy;
            return this;
        }

        /**
         * Sets the subject to deliver messages to.
         * @param subject the delivery subject.
         * @return the builder
         */
        public Builder deliverSubject(String subject) {
            this.deliverSubject = subject;
            return this;
        }  

        /**
         * Sets the start sequence of the ConsumerConfiguration.
         * @param sequence the start sequence
         * @return Builder
         */
        public Builder startSequence(long sequence) {
            this.startSeq = sequence;
            return this;
        }

        /**
         * Sets the start time of the ConsumerConfiguration.
         * @param startTime the start time
         * @return Builder
         */        
        public Builder startTime(ZonedDateTime startTime) {
            this.startTime = startTime;
            return this;
        }

        /**
         * Sets the acknowledgement policy of the ConsumerConfiguration.
         * @param policy the acknowledgement policy.
         * @return Builder
         */        
        public Builder ackPolicy(AckPolicy policy) {
            this.ackPolicy = policy;
            return this;
        }

        /**
         * Sets the acknowledgement wait duration of the ConsumerConfiguration.
         * @param timeout the wait timeout
         * @return Builder
         */ 
        public Builder ackWait(Duration timeout) {
            this.ackWait = timeout;
            return this;
        }

        /**
         * Sets the maximum delivery amount of the ConsumerConfiguration.
         * @param maxDeliver the maximum delivery amount
         * @return Builder
         */        
        public Builder maxDeliver(long maxDeliver) {
            this.maxDeliver = maxDeliver;
            return this;
        }

        /**
         * Sets the filter subject of the ConsumerConfiguration.
         * @param filterSubject the filter subject
         * @return Builder
         */         
        public Builder filterSubject(String filterSubject) {
            this.filterSubject = filterSubject;
            return this;
        }

        /**
         * Sets the replay policy of the ConsumerConfiguration.
         * @param policy the replay policy.
         * @return Builder
         */         
        public Builder replayPolicy(ReplayPolicy policy) {
            this.replayPolicy = policy;
            return this;
        }

        /**
         * Sets the sample frequency of the ConsumerConfiguration.
         * @param frequency the frequency
         * @return Builder
         */
        public Builder sampleFrequency(String frequency) {
            this.sampleFrequency = frequency;
            return this;
        }

        /**
         * Set the rate limit of the ConsumerConfiguration.
         * @param msgsPerSecond messages per second to deliver
         * @return Builder
         */
        public Builder rateLimit(int msgsPerSecond) {
            this.rateLimit = msgsPerSecond;
            return this;
        }

        /**
         * Sets the maximum ack pending.
         * @param maxAckPending maximum pending acknowledgements.
         * @return Builder
         */
        public Builder maxAckPending(long maxAckPending) {
            this.maxAckPending = maxAckPending;
            return this;
        }

        /**
         * Builds the ConsumerConfiguration
         * @return a consumer configuration.
         */
        public ConsumerConfiguration build() {
            return new ConsumerConfiguration(
                durable,
                deliverPolicy,
                startSeq,
                startTime,
                ackPolicy,
                ackWait,
                maxDeliver,
                filterSubject,
                replayPolicy,
                sampleFrequency,
                rateLimit,
                deliverSubject,
                maxAckPending
            );
        }
    }

    @Override
    public String toString() {
        return "ConsumerConfiguration{" +
                "durable='" + durable + '\'' +
                ", deliverPolicy=" + deliverPolicy +
                ", deliverSubject='" + deliverSubject + '\'' +
                ", startSeq=" + startSeq +
                ", startTime=" + startTime +
                ", ackPolicy=" + ackPolicy +
                ", ackWait=" + ackWait +
                ", maxDeliver=" + maxDeliver +
                ", filterSubject='" + filterSubject + '\'' +
                ", replayPolicy=" + replayPolicy +
                ", sampleFrequency='" + sampleFrequency + '\'' +
                ", rateLimit=" + rateLimit +
                ", maxAckPending=" + maxAckPending +
                '}';
    }
}
