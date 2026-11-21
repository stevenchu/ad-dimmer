import org.stevenchu.screencover.MediaMute;

public final class MediaMuteTest {
  private static final class Fixture implements MediaMute.Audio, MediaMute.Recovery {
    boolean muted, pending, ignoreChange, failChange, failSave;
    int changes;
    MediaMute session = new MediaMute(this, this);

    public boolean isMuted() {
      return muted;
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

    public void save(boolean value) {
      if (failSave) throw new IllegalStateException("Storage unavailable");
      pending = value;
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
    check(f.muted && f.pending, "Cover mutes and records recovery");
    f.session.restore();
    check(!f.muted && !f.pending, "Uncover restores and clears recovery");
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
    f.session.restore();
    check(f.changes == 1 && !f.pending, "User unmute is preserved during cleanup");

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
    System.out.println("MediaMute tests passed");
  }
}
