package de.drowsydriveralarm.eventproducer;

import com.google.common.eventbus.EventBus;

import de.drowsydriveralarm.Clock;
import de.drowsydriveralarm.EventProducingGraphicFaceTracker;
import de.drowsydriveralarm.listener.EventLogger;

public class DrowsyEventDetector {

    private final EventBus eventBus;
    private final DrowsyEventProducer drowsyEventProducer;
    private final EventProducingGraphicFaceTracker eventProducingGraphicFaceTracker;

    public DrowsyEventDetector(final DrowsyEventDetectorConfig drowsyEventDetectorConfig, final boolean registerEventLogger, final Clock clock) {
        this.eventBus = new EventBus();
        if (registerEventLogger) {
            this.eventBus.register(new EventLogger());
        }
        this.eventBus.register(new EyesOpenedEventProducer(drowsyEventDetectorConfig.getEyeOpenProbabilityThreshold(), this.eventBus));
        this.eventBus.register(new EyesClosedEventProducer(drowsyEventDetectorConfig.getEyeOpenProbabilityThreshold(), this.eventBus));
        this.eventBus.register(new NormalEyeBlinkEventProducer(drowsyEventDetectorConfig.getSlowEyelidClosureMinDuration(), this.eventBus));
        this.eventBus.register(new SlowEyelidClosureEventProducer(drowsyEventDetectorConfig.getSlowEyelidClosureMinDuration(), this.eventBus));
        this.eventBus.register(new PendingSlowEyelidClosureEventProducer(drowsyEventDetectorConfig.getEyeOpenProbabilityThreshold(), drowsyEventDetectorConfig.getSlowEyelidClosureMinDuration(), this.eventBus));
        final SlowEyelidClosureEventsProvider slowEyelidClosureEventsProvider = new SlowEyelidClosureEventsProvider(drowsyEventDetectorConfig.getTimeWindow());
        this.eventBus.register(slowEyelidClosureEventsProvider);

        this.drowsyEventProducer = new DrowsyEventProducer(drowsyEventDetectorConfig.getConfig(), this.eventBus, slowEyelidClosureEventsProvider);
        this.eventProducingGraphicFaceTracker = new EventProducingGraphicFaceTracker(this.eventBus, this.drowsyEventProducer, clock);
    }

    public EventBus getEventBus() {
        return this.eventBus;
    }

    public EventProducingGraphicFaceTracker getEventProducingGraphicFaceTracker() {
        return this.eventProducingGraphicFaceTracker;
    }

    public DrowsyEventProducer getDrowsyEventProducer() {
        return this.drowsyEventProducer;
    }
}