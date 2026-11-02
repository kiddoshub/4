
package com.gaspoweredcake.client33.utils.files;

import com.gaspoweredcake.client33.Client33;
import org.apache.commons.io.IOUtils;

import java.io.*;

public class StreamUtils {
    private StreamUtils() {
    }

    public static void copy(File from, File to) {
        try (InputStream in = new FileInputStream(from);
             OutputStream out = new FileOutputStream(to)) {
            in.transferTo(out);
        } catch (IOException e) {
            Client33.LOG.error("Error copying from file '{}' to file '{}'.", from.getName(), to.getName(), e);
        }
    }

    public static void copy(InputStream in, File to) {
        try (OutputStream out = new FileOutputStream(to)) {
            in.transferTo(out);
        } catch (IOException _) {
            Client33.LOG.error("Error writing to file '{}'.", to.getName());
        } finally {
            IOUtils.closeQuietly(in);
        }
    }
}
