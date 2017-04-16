package de.drowsydriveralarm.eventproducer;

import android.support.annotation.NonNull;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.Landmark;
import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;

import org.joda.time.Instant;

import java.util.Arrays;
import java.util.List;

import de.drowsydriveralarm.Clock;
import de.drowsydriveralarm.event.EventHelper;
import de.drowsydriveralarm.event.UpdateEvent;

public class EventProducingGraphicFaceTracker extends Tracker<Face> {

    private final EventBus eventBus;
    private final DrowsyEventProducer drowsyEventProducer;
    private final Clock clock;

    private ClockTime2FrameTimeConverter timeConverter;

    public EventProducingGraphicFaceTracker(final EventBus eventBus, final DrowsyEventProducer drowsyEventProducer, final Clock clock) {
        this.eventBus = eventBus;
        this.drowsyEventProducer = drowsyEventProducer;
        this.clock = clock;
    }

    @Override
    public void onUpdate(final Detector.Detections<Face> detections, final Face face) {
        // TODO: Falls über einen in den Settings zu konfigurierenden Zeitraum beide Augen nicht erkannt werden,
        // soll DrowsyDriverAlarm außer Betrieb gesetzt werden.
        // TODO: use RetroLambda (https://github.com/orfjackal/retrolambda)
        final Instant clockTime = this.clock.now();
        if (this.timeConverter == null) {
            this.timeConverter = ClockTime2FrameTimeConverter.fromClockTimeAndFrameTime(clockTime, EventHelper.getInstantOf(detections));
        }

        if (!this.areBothEyesRecognized(face)) {
            return;
        }

        this.eventBus.post(new UpdateEvent(detections, face));
        this.drowsyEventProducer.maybeProduceDrowsyEvent(this.timeConverter.convertToFrameTime(clockTime));
    }

    private boolean areBothEyesRecognized(final Face face) {
        return this.getLandmarkTypes(face.getLandmarks()).containsAll(this.getBothEyes());
    }

    private ImmutableList<Integer> getLandmarkTypes(final List<Landmark> landmarks) {
        return FluentIterable
                .from(landmarks)
                .transform(
                        new Function<Landmark, Integer>() {

                            @Override
                            public Integer apply(final Landmark landmark) {
                                return landmark.getType();
                            }
                        })
                .toList();
    }

    @NonNull
    private List<Integer> getBothEyes() {
        return Arrays.asList(Landmark.LEFT_EYE, Landmark.RIGHT_EYE);
    }
}
