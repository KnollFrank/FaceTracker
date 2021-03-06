package de.drowsydriveralarm.eventproducer;

import com.google.common.base.Optional;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import org.joda.time.Duration;
import org.joda.time.Instant;

import de.drowsydriveralarm.event.EyesClosedEvent;
import de.drowsydriveralarm.event.EyesOpenedEvent;
import de.drowsydriveralarm.event.PendingSlowEyelidClosureEvent;
import de.drowsydriveralarm.event.UpdateEvent;

import static de.drowsydriveralarm.eventproducer.SlowEyelidClosureEventProducer.isSlowEyelidClosure;

public class PendingSlowEyelidClosureEventProducer extends EventProducer {

    private final Duration slowEyelidClosureMinDuration;
    private final float eyeOpenProbabilityThreshold;
    private Optional<Instant> eyesClosed = Optional.absent();

    public PendingSlowEyelidClosureEventProducer(final float eyeOpenProbabilityThreshold, final Duration slowEyelidClosureMinDuration, final EventBus eventBus) {
        super(eventBus);
        this.eyeOpenProbabilityThreshold = eyeOpenProbabilityThreshold;
        this.slowEyelidClosureMinDuration = slowEyelidClosureMinDuration;
    }

    @Subscribe
    public void recordEyesClosed(final EyesClosedEvent eyesClosedEvent) {
        this.eyesClosed = Optional.of(eyesClosedEvent.getInstant());
    }

    @Subscribe
    public void removeEyesClosed(final EyesOpenedEvent eyesOpenedEvent) {
        this.eyesClosed = Optional.absent();
    }

    @Subscribe
    public void maybePostPendingSlowEyelidClosureEvent(final UpdateEvent updateEvent) {
        if (!this.eyesClosed.isPresent()) {
            return;
        }

        final Duration duration = new Duration(this.eyesClosed.get(), updateEvent.getInstant());
        if (!EyesOpenedEventProducer.isEyesOpen(updateEvent.getFace(), this.eyeOpenProbabilityThreshold) && isSlowEyelidClosure(duration, this.slowEyelidClosureMinDuration)) {
            this.postEvent(new PendingSlowEyelidClosureEvent(this.eyesClosed.get(), duration));
        }
    }
}
