package org.stevenchu.screencover;

/** Restores only a media mute introduced by Ad Dimmer, including after a service restart. */
public final class MediaMute {
  public interface Audio {
    boolean isMuted();

    int volume();

    void setVolume(int volume);

    void setMuted(boolean muted);
  }

  public interface Recovery {
    boolean pending();

    int savedVolume();

    // Implementations must persist before returning and throw if the write fails.
    void save(boolean pending, int volume);
  }

  private final Audio audio;
  private final Recovery recovery;

  public MediaMute(Audio audio, Recovery recovery) {
    this.audio = audio;
    this.recovery = recovery;
  }

  public boolean pending() {
    return recovery.pending();
  }

  public void mute() {
    // Finish an interrupted session before taking a new snapshot.
    restore();
    if (audio.isMuted()) return;
    int volume = audio.volume();
    if (volume == 0) return;
    recovery.save(true, volume);
    // Some TV firmware reports a muted stream while audio remains audible. Zero the volume too.
    audio.setVolume(0);
    if (audio.volume() != 0) throw new IllegalStateException("Android media volume did not mute");
  }

  public void restore() {
    if (!recovery.pending()) return;
    int volume = recovery.savedVolume();
    if (volume < 0) {
      // Recover journals written by the earlier mute-flag-only preview.
      if (audio.isMuted()) audio.setMuted(false);
      if (audio.isMuted()) throw new IllegalStateException("Android media audio did not restore");
    } else if (audio.volume() == 0) {
      audio.setVolume(volume);
      if (audio.volume() != volume) {
        throw new IllegalStateException("Android media volume did not restore");
      }
    }
    // A nonzero volume changed while covered belongs to the user, so preserve it.
    recovery.save(false, -1);
  }
}
