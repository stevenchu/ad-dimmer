import org.stevenchu.screencover.MediaMute;

public final class MediaMuteTest {
  private static final class Fixture implements MediaMute.Audio, MediaMute.Recovery {
    boolean muted, pending, ignoreChange, failChange, failSave;
    int changes, volume = 33, savedVolume = -1;
    MediaMute session = new MediaMute(this, this);

    public boolean isMuted() {
      return muted;
    }

    public int volume() {
      return muted ? 0 : volume;
    }

    public int savedVolume() {
      return savedVolume;
    }

    public void setVolume(int value) {
      if (!pending) throw new AssertionError("Audio changed before recovery was saved");
      changes++;
      if (failChange) throw new IllegalStateException("Audio unavailable");
      if (!ignoreChange) {
        volume = value;
        muted = value == 0;
      }
    }

    public boolean pending() {
      return pending;
    }

    public void setMuted(boolean value) {
      if (!pending) throw new AssertionError("Audio changed before recovery was saved");
      changes++;
      if (failChange) throw new IllegalStateException("Audio unavailable");
      if (!ignoreChange) muted = value;
    }

    public void save(boolean value, int originalVolume) {
      if (failSave) throw new IllegalStateException("Storage unavailable");
      pending = value;
      savedVolume = originalVolume;
    }
  }

  private static void check(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }

  private static void fails(Runnable operation) {
    try {
      operation.run();
    } catch (IllegalStateException expected) {
      return;
    }
    throw new AssertionError("Failure was not reported");
  }

  public static void main(String[] args) {
    Fixture f = new Fixture();
    f.session.mute();
    check(
        f.muted && f.pending && f.volume == 0 && f.savedVolume == 33,
        "Cover zeros volume and records recovery");
    f.session.restore();
    check(!f.muted && !f.pending && f.volume == 33, "Uncover restores volume and clears recovery");
    f.session.restore();
    check(f.changes == 2, "Repeated lifecycle cleanup leaves audio alone");

    f = new Fixture();
    f.muted = true;
    f.session.mute();
    f.session.restore();
    check(f.muted && !f.pending && f.changes == 0, "Already-muted audio stays muted");

    f = new Fixture();
    f.session.mute();
    f.session = new MediaMute(f, f);
    f.session.restore();
    check(!f.muted && !f.pending, "A new service restores an interrupted session");

    f = new Fixture();
    f.session.mute();
    f.muted = false;
    f.volume = 20;
    f.session.restore();
    check(
        f.changes == 1 && !f.pending && f.volume == 20, "User unmute is preserved during cleanup");

    f = new Fixture();
    f.failSave = true;
    fails(f.session::mute);
    check(!f.muted && f.changes == 0, "Failed persistence prevents muting");

    f = new Fixture();
    f.ignoreChange = true;
    fails(f.session::mute);
    check(!f.muted && f.pending, "Unsupported output reports failure");
    f.session.restore();
    check(!f.pending, "No-op mute can be cleaned up");

    f = new Fixture();
    f.session.mute();
    f.failChange = true;
    fails(f.session::restore);
    check(f.pending && f.muted, "Failed restoration retains recovery");
    f.failChange = false;
    f.session.restore();
    check(!f.pending && !f.muted, "Restoration can be retried");

    f = new Fixture();
    f.failChange = true;
    fails(f.session::mute);
    check(f.pending, "Failed audio request retains recovery in case it changed audio");
    f.failChange = false;
    f.session.mute();
    f.session.restore();
    check(!f.pending && !f.muted, "A later cover recovers before beginning");

    f = new Fixture();
    f.session.mute();
    f.failSave = true;
    fails(f.session::restore);
    check(!f.muted && f.pending, "Failed journal cleanup retains safe retry");
    f.failSave = false;
    f.session.restore();
    check(!f.pending && !f.muted, "Cleanup retry preserves restored audio");
    f = new Fixture();
    f.volume = 0;
    f.session.mute();
    f.session.restore();
    check(f.volume == 0 && f.changes == 0 && !f.pending, "Silent starting volume stays silent");

    f = new Fixture();
    f.pending = true;
    f.muted = true;
    f.session.restore();
    check(!f.muted && !f.pending && f.volume == 33, "Legacy mute-only journal recovers");

    System.out.println("MediaMute tests passed");
  }
}
