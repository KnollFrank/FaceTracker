package de.drowsydriveralarm.eventproducer;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.common.base.Optional;
import com.google.common.eventbus.EventBus;

import de.drowsydriveralarm.Clock;
import de.drowsydriveralarm.event.AppActiveEvent;
import de.drowsydriveralarm.event.AppIdleEvent;

public class FaceTrackingActiveAndIdleEventProducer extends Tracker<Face> {

    private final EventBus eventBus;
    private final Clock clock;
    private Optional<Boolean> active = Optional.absent();

    public FaceTrackingActiveAndIdleEventProducer(final EventBus eventBus, final Clock clock) {
        this.eventBus = eventBus;
        this.clock = clock;
    }

    @Override
    public void onNewItem(final int i, final Face face) {
        if(this.isUnknown() || this.isIdle()) {
            this.setActive();
            this.eventBus.post(new AppActiveEvent(this.clock.now()));
        }
    }

    @Override
    public void onUpdate(final Detector.Detections<Face> detections, final Face face) {
        if (this.isUnknown() || this.isIdle()) {
            this.setActive();
            this.eventBus.post(new AppActiveEvent(this.clock.now()));
        }
    }

    @Override
    public void onMissing(final Detector.Detections<Face> detections) {
        if (this.isUnknown() || this.isActive()) {
            this.setIdle();
            this.eventBus.post(new AppIdleEvent(this.clock.now()));
        }
    }

    @Override
    public void onDone() {
        if (this.isUnknown() || this.isActive()) {
            this.setIdle();
            this.eventBus.post(new AppIdleEvent(this.clock.now()));
        }
    }

    private boolean isUnknown() {
        return !this.active.isPresent();
    }

    private Boolean isActive() {
        return this.active.get();
    }

    private void setActive() {
        this.active = Optional.of(true);
    }

    private boolean isIdle() {
        return !this.isActive();
    }

    private void setIdle() {
        this.active = Optional.of(false);
    }
}