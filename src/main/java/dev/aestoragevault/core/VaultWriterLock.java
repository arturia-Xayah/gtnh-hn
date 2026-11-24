package dev.aestoragevault.core;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;

/** Keep ownership for the lifetime of a mounted vault, independently of its backup files. */
final class VaultWriterLock implements Closeable {

    private static final Set<Path> mounted = new HashSet<>();
    private final Path path;
    private final FileChannel channel;

    private VaultWriterLock(Path path, FileChannel channel) {
        this.path = path;
        this.channel = channel;
    }

    static VaultWriterLock acquire(Path file) throws IOException {
        Files.createDirectories(file.getParent());
        Path path = file.getParent()
            .toRealPath()
            .resolve(file.getFileName());
        synchronized (mounted) {
            // On POSIX even closing a second descriptor can release this process's locks.
            if (!mounted.add(path)) throw new OverlappingFileLockException();
            try {
                FileChannel channel = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                try {
                    // Beyond EOF so a reader that also backs up runtime files can read this empty marker on Windows.
                    if (channel.tryLock(65536, 1, false) == null)
                        throw new IOException("Inventory already open in another process: " + path);
                    return new VaultWriterLock(path, channel);
                } catch (IOException | RuntimeException | Error e) {
                    channel.close();
                    throw e;
                }
            } catch (IOException | RuntimeException | Error e) {
                mounted.remove(path);
                throw e;
            }
        }
    }

    @Override
    public void close() throws IOException {
        synchronized (mounted) {
            if (!channel.isOpen()) return;
            channel.close();
            mounted.remove(path);
            // Leave the empty file in place: deleting it can split ownership across two inodes.
        }
    }
}
