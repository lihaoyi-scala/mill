package mill.main.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Optional;

public class FileToStreamTailer extends Thread implements AutoCloseable {

    private final File file;
    private final PrintStream stream;
    private final int intervalMsec;

    private Optional<BufferedReader> reader = Optional.empty();
    // if true, we won't read the whole file, but only new lines
    private boolean ignoreHead = true;

    private volatile boolean keepReading = true;
    private volatile boolean flush = false;

    public FileToStreamTailer(File file, PrintStream stream, int intervalMsec) {
        super("Tail");
        this.intervalMsec = intervalMsec;
        setDaemon(true);
        this.file = file;
        this.stream = stream;
    }

    @Override
    public void run() {
        if (isInterrupted()) {
            keepReading = false;
        }
        while (keepReading || flush) {
            flush = false;
            try {
                if (!reader.isPresent()) {
                    try {
                        this.reader = Optional.of(new BufferedReader(new FileReader(file)));
                    } catch (FileNotFoundException e) {
                        // nothing to ignore if file is inially missing
                        ignoreHead = false;
                    }
                }
                reader.ifPresent(r -> {
                    // read lines
                    try {
                        String line;
                        while ((line = r.readLine()) != null) {
                            if (!ignoreHead) {
                                stream.println(line);
                            }
                        }
                        // we ignored once
                        this.ignoreHead = false;
                    } catch (FileNotFoundException e) {
                        // File vanished
                        this.reader = Optional.empty();
                        this.ignoreHead = false;
                    } catch (IOException e) {
                        // could not read line or file vanished
                    }
                });
            } finally {
                if (keepReading) {
                    // wait
                    try {
                        Thread.sleep(intervalMsec);
                    } catch (InterruptedException e) {
                        // can't handle anyway
                    }
                }
            }
        }
    }

    @Override
    public void interrupt() {
        this.keepReading = false;
        super.interrupt();
    }

    /**
     * Force a next read, even if we interrupt the thread.
     */
    public void flush() {
        this.flush = true;
    }

    @Override
    public void close() throws Exception {
        interrupt();
    }
}
