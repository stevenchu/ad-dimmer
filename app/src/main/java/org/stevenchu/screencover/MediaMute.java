package org.stevenchu.screencover;

/** Restores only a media mute introduced by Ad Dimmer, including after a service restart. */
public final class MediaMute {
  public interface Audio {
    boolean isMuted();

    void setMuted(boolean muted);
  }

  public interface Recovery {
    boolean pending();

    // Implementations must persist before returning and throw if the write fails.
    void save(boolean pending);
  }

  private final Audio audio;
  private final Recovery recovery;

  public MediaMute(Audio audio, Recovery recovery) {
    this.audio = audio;
    this.recovery = recovery;
  }

  public void mute() {
    // Finish an interrupted session before taking a new snapshot.
    restore();
    if (audio.isMuted()) return;
    recovery.save(true);
    audio.setMuted(true);
    if (!audio.isMuted()) throw new IllegalStateException("Android media audio did not mute");
  }

  public void restore() {
    if (!recovery.pending()) return;
    if (audio.isMuted()) audio.setMuted(false);
    if (audio.isMuted()) throw new IllegalStateException("Android media audio did not restore");
    recovery.save(false);
  }
}
