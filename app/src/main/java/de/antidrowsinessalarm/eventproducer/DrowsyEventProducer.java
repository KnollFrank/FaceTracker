package de.antidrowsinessalarm.eventproducer;

import android.support.annotation.NonNull;

import com.google.common.eventbus.EventBus;

import org.joda.time.Instant;

import de.antidrowsinessalarm.PERCLOSCalculator;
import de.antidrowsinessalarm.event.AwakeEvent;
import de.antidrowsinessalarm.event.DrowsyEvent;
import de.antidrowsinessalarm.event.LikelyDrowsyEvent;

public class DrowsyEventProducer extends EventProducer {

    private final Config config;
    private final SlowEyelidClosureEventsProvider slowEyelidClosureEventsProvider;

    public DrowsyEventProducer(final Config config, final EventBus eventBus, final SlowEyelidClosureEventsProvider slowEyelidClosureEventsProvider) {
        super(eventBus);
        this.config = config;
        this.slowEyelidClosureEventsProvider = slowEyelidClosureEventsProvider;
    }

    public void maybeProduceDrowsyEvent(final Instant now) {
        double perclos = this.getPerclos(now);
        if(perclos >= this.config.getDrowsyThreshold()) {
            this.postEvent(new DrowsyEvent(now, perclos));
        } else if(perclos >= this.config.getLikelyDrowsyThreshold()) {
            this.postEvent(new LikelyDrowsyEvent(now, perclos));
        } else {
            this.postEvent(new AwakeEvent(now, perclos));
        }
    }

    private double getPerclos(final Instant now) {
        return this.getPERCLOSCalculator().calculatePERCLOS(this.slowEyelidClosureEventsProvider.getRecordedEventsPartlyWithinTimeWindow(now), now);
    }

    @NonNull
    private PERCLOSCalculator getPERCLOSCalculator() {
        return new PERCLOSCalculator(this.slowEyelidClosureEventsProvider.getTimeWindow());
    }

    public static class Config {

        private final double drowsyThreshold; // = 0.15;
        private final double likelyDrowsyThreshold; // = 0.08;

        private Config(final double drowsyThreshold, final double likelyDrowsyThreshold) {
            this.drowsyThreshold = drowsyThreshold;
            this.likelyDrowsyThreshold = likelyDrowsyThreshold;
        }

        public static ConfigBuilder builder() {
            return new ConfigBuilder();
        }

        public double getDrowsyThreshold() {
            return this.drowsyThreshold;
        }

        public double getLikelyDrowsyThreshold() {
            return this.likelyDrowsyThreshold;
        }

        public static class ConfigBuilder {

            private double drowsyThreshold;
            private double likelyDrowsyThreshold;

            public ConfigBuilder setDrowsyThreshold(final double drowsyThreshold) {
                this.drowsyThreshold = drowsyThreshold;
                return this;
            }

            public ConfigBuilder setLikelyDrowsyThreshold(final double likelyDrowsyThreshold) {
                this.likelyDrowsyThreshold = likelyDrowsyThreshold;
                return this;
            }

            public Config build() {
                return new Config(this.drowsyThreshold, this.likelyDrowsyThreshold);
            }
        }
    }
}
