package com.google.android.gms.samples.vision.face.facetracker;

import com.google.android.gms.samples.vision.face.facetracker.event.UpdateEvent;
import com.google.android.gms.samples.vision.face.facetracker.listener.DrowsyEventProducer;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.common.eventbus.EventBus;

public class GraphicFaceTracker extends Tracker<Face> {

    private final EventBus eventBus;
    private final DrowsyEventProducer drowsyEventProducer;
    private long delta;
    private boolean firstCallToOnUpdate = true;

    public GraphicFaceTracker(final EventBus eventBus, final DrowsyEventProducer drowsyEventProducer) {
        this.eventBus = eventBus;
        this.drowsyEventProducer = drowsyEventProducer;
    }

    @Override
    public void onUpdate(Detector.Detections<Face> detections, Face face) {
        if(this.firstCallToOnUpdate) {
            this.delta = detections.getFrameMetadata().getTimestampMillis() - System.currentTimeMillis();
            this.firstCallToOnUpdate = false;
        }
        this.eventBus.post(new UpdateEvent(detections, face));
        this.drowsyEventProducer.maybeProduceDrowsyEvent(System.currentTimeMillis() + this.delta);
    }
}
