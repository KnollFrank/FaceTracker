package de.drowsydriveralarm.eventproducer;

import com.google.common.eventbus.EventBus;

import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;

import de.drowsydriveralarm.EventListener;
import de.drowsydriveralarm.SystemClock;
import de.drowsydriveralarm.event.AwakeEvent;
import de.drowsydriveralarm.event.DrowsyEvent;
import de.drowsydriveralarm.event.Event;
import de.drowsydriveralarm.event.LikelyDrowsyEvent;
import de.drowsydriveralarm.event.SlowEyelidClosureEvent;
import de.drowsydriveralarm.event.UpdateEvent;

import static de.drowsydriveralarm.eventproducer.VisionHelper.createFaceWithEyesClosed;
import static de.drowsydriveralarm.eventproducer.VisionHelper.createFaceWithEyesOpened;
import static de.drowsydriveralarm.eventproducer.VisionHelper.getFaceDetections;
import static junit.framework.Assert.fail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.Is.isA;
import static org.hamcrest.core.IsNot.not;

public class DrowsyEventProducerTest {

    private EventListener listener;
    private EventBus eventBus;
    private DrowsyEventProducer drowsyEventProducer;

    @Before
    public void setup() {
        final IDrowsyEventDetectorConfig configFactory = new TestingDrowsyEventDetectorConfig(SharedPreferencesTestFactory.createSharedPreferences());
        final DrowsyEventDetector drowsyEventDetector =
                new DrowsyEventDetector(
                        DrowsyEventDetectorConfig
                                .builder()
                                .withEyeOpenProbabilityThreshold(configFactory.getEyeOpenProbabilityThreshold())
                                .withConfig(configFactory.getConfig())
                                .withSlowEyelidClosureMinDuration(configFactory.getSlowEyelidClosureMinDuration())
                                .withTimeWindow(new Duration(2000))
                                .build(),
                        false,
                        new SystemClock()
                );
        this.listener = new EventListener();
        this.eventBus = drowsyEventDetector.getEventBus();
        this.drowsyEventProducer = drowsyEventDetector.getDrowsyEventProducer();
        this.eventBus.register(this.listener);
    }

    @Test
    public void shouldCreateDrowsyEvent() {
        // Given
        this.eventBus.post(new SlowEyelidClosureEvent(new Instant(100), new Duration(600)));
        this.eventBus.post(new SlowEyelidClosureEvent(new Instant(1000), new Duration(550)));
        final double perclos = (600.0 + 550.0) / 2000.0; // = 0.575 > 0.15

        // When
        this.drowsyEventProducer.maybeProduceDrowsyEvent(new Instant(2000));

        // Then
        assertThat(this.listener.getEvent(), is((Event) new DrowsyEvent(new Instant(2000), perclos)));
    }

    @Test
    public void shouldCreateDrowsyEventForEyesClosedTheWholeTime() {
        // Given
        this.eventBus.post(new UpdateEvent(getFaceDetections(new Instant(0)), createFaceWithEyesClosed()));
        this.eventBus.post(new UpdateEvent(getFaceDetections(new Instant(2000)), createFaceWithEyesClosed()));
        final double perclos = 1.0; // > 0.15

        // When
        this.drowsyEventProducer.maybeProduceDrowsyEvent(new Instant(2000));

        // Then
        assertThat(this.listener.getEvent(), is((Event) new DrowsyEvent(new Instant(2000), perclos)));
    }

    // TODO: test mit IndefiniteEyeState dazwischenmogeln

    @Test
    public void shouldCreateDrowsyEventForEyesClosedButNotYetOpenedWhenMaybeProducingDrowsyEvent() {
        // Given
        this.eventBus.post(new UpdateEvent(getFaceDetections(new Instant(0)), createFaceWithEyesClosed()));
        this.eventBus.post(new UpdateEvent(getFaceDetections(new Instant(501)), createFaceWithEyesOpened()));
        this.eventBus.post(new UpdateEvent(getFaceDetections(new Instant(510)), createFaceWithEyesClosed()));
        this.eventBus.post(new UpdateEvent(getFaceDetections(new Instant(2000)), createFaceWithEyesClosed()));
        final double perclos = (501.0 + (2000.0 - 510.0)) / 2000.0; // = 0.9955  > 0.15

        // When
        this.drowsyEventProducer.maybeProduceDrowsyEvent(new Instant(2000));

        // Then
        assertThat(this.listener.getEvent(), is((Event) new DrowsyEvent(new Instant(2000), perclos)));
    }

    @Test
    public void shouldCreateDrowsyEventForEyesClosedButNotYetOpenedWhenMaybeProducingDrowsyEvent2() {
        // Given
        this.eventBus.post(new UpdateEvent(getFaceDetections(new Instant(0)), createFaceWithEyesClosed()));
        this.eventBus.post(new UpdateEvent(getFaceDetections(new Instant(501)), createFaceWithEyesOpened()));
        this.eventBus.post(new UpdateEvent(getFaceDetections(new Instant(510)), createFaceWithEyesClosed()));
        this.eventBus.post(new UpdateEvent(getFaceDetections(new Instant(1000)), createFaceWithEyesClosed()));
        this.eventBus.post(new UpdateEvent(getFaceDetections(new Instant(2000)), createFaceWithEyesClosed()));
        final double perclos = (501.0 + (2000.0 - 510.0)) / 2000.0; // = 0.9955  > 0.15

        // When
        this.drowsyEventProducer.maybeProduceDrowsyEvent(new Instant(2000));

        // Then
        assertThat(this.listener.getEvents(), hasItem(new DrowsyEvent(new Instant(2000), perclos)));
    }

    @Test
    public void shouldCreateLikelyDrowsyEvent() {
        // Given
        this.eventBus.post(new SlowEyelidClosureEvent(new Instant(100), new Duration(150)));
        this.eventBus.post(new SlowEyelidClosureEvent(new Instant(1000), new Duration(55)));
        final double perclos = (150 + 55.0) / 2000.0; // = 0.1025 which is between 0.08 and 0.15

        // When
        this.drowsyEventProducer.maybeProduceDrowsyEvent(new Instant(2000));

        // Then
        assertThat(this.listener.getEvent(), is((Event) new LikelyDrowsyEvent(new Instant(2000), perclos)));
    }

    @Test
    public void shouldCreateAwakeEvent() {
        // Given

        // When
        this.drowsyEventProducer.maybeProduceDrowsyEvent(new Instant(2000));

        // Then
        assertThat(this.listener.getEvent(), is((Event) new AwakeEvent(new Instant(2000), 0)));
    }

    @Test
    public void shouldCreateNoDrowsyEvents() {
        // Given
        this.eventBus.post(new SlowEyelidClosureEvent(new Instant(100), new Duration(150)));
        this.eventBus.post(new SlowEyelidClosureEvent(new Instant(1000), new Duration(55)));

        // When
        // given SlowEyelidClosureEvents are not within time window
        this.drowsyEventProducer.maybeProduceDrowsyEvent(new Instant(5000));

        // Then
        assertThat(this.listener.getEvents(), not(hasItem(isA(LikelyDrowsyEvent.class))));
        assertThat(this.listener.getEvents(), not(hasItem(isA(DrowsyEvent.class))));
    }

    // @Test
    public void shouldCreateASingleDrowsyEvent() {
        fail("not yet implemented");
    }

    // @Test
    public void shouldCreateASingleLikelyDrowsyEvent() {
        fail("not yet implemented");
    }

    // @Test
    public void shouldCreateASingleAwakeEvent() {
        fail("not yet implemented");
    }
}
