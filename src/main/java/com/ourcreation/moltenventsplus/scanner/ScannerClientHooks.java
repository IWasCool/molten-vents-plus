package com.ourcreation.moltenventsplus.scanner;

import java.util.Objects;
import java.util.function.Consumer;
import com.ourcreation.moltenventsplus.vent.VentTier;

/** Client callbacks are installed by the client subscriber; this class is safe on a dedicated server. */
public final class ScannerClientHooks {
    private static Consumer<VentTier> open = value -> {};
    private static Runnable stop = () -> {};
    private static Consumer<ScannerNetwork.Result> result = value -> {};

    public static void setOpen(Consumer<VentTier> callback) { open = Objects.requireNonNull(callback); }
    public static void setResult(Consumer<ScannerNetwork.Result> callback) { result = Objects.requireNonNull(callback); }
    public static void setStop(Runnable callback) { stop = Objects.requireNonNull(callback); }
    public static void open(VentTier tier) { open.accept(tier); }
    public static void stop() { stop.run(); }
    static void accept(ScannerNetwork.Result value) { result.accept(value); }

    private ScannerClientHooks() {}
}
