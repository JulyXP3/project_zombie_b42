package modcore.agent;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.file.Path;
import java.security.CodeSource;
import java.security.cert.Certificate;
import java.util.Collections;
import java.util.Enumeration;

/** One application type universe: verified game JAR plus separately sourced payload classes. */
public final class PayloadClassLoader extends URLClassLoader {
    static { registerAsParallelCapable(); }

    private final PayloadArchive archive;
    private final CodeSource payloadSource;

    public PayloadClassLoader(Path gameJar, PayloadArchive archive, ClassLoader parent) throws IOException {
        super("modcore-application", new URL[]{gameJar.toUri().toURL()}, parent);
        this.archive = archive;
        this.payloadSource = new CodeSource(archive.source().toUri().toURL(), (Certificate[]) null);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (!isPayloadName(name)) return super.loadClass(name, resolve);
        synchronized (getClassLoadingLock(name)) {
            Class<?> type = findLoadedClass(name);
            if (type == null) type = findClass(name);
            if (resolve) resolveClass(type);
            return type;
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (!isPayloadName(name)) return super.findClass(name);
        byte[] bytes = archive.classBytes(name);
        if (bytes == null) throw new ClassNotFoundException("Payload class missing: " + name);
        int split = name.lastIndexOf('.');
        if (split > 0) {
            String packageName = name.substring(0, split);
            if (getDefinedPackage(packageName) == null) {
                try {
                    definePackage(packageName, null, null, null, null, null, null, null);
                } catch (IllegalArgumentException e) {
                    if (getDefinedPackage(packageName) == null) throw e;
                }
            }
        }
        return defineClass(name, bytes, 0, bytes.length, payloadSource);
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        if (isPayloadResource(name)) return archive.open(name);
        return super.getResourceAsStream(name);
    }

    @Override
    public URL getResource(String name) {
        if (!isPayloadResource(name)) return super.getResource(name);
        return payloadUrl(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        if (!isPayloadResource(name)) return super.getResources(name);
        URL url = payloadUrl(name);
        return Collections.enumeration(url == null ? java.util.List.of() : java.util.List.of(url));
    }

    public java.util.List<String> resourceNames(String prefix) {
        return archive.resourceNames(prefix);
    }

    private URL payloadUrl(String name) {
        try (InputStream in = archive.open(name)) {
            if (in == null) return null;
            return new URL(null, "modcore-payload:/" + name, new URLStreamHandler() {
                @Override protected URLConnection openConnection(URL url) {
                    return new URLConnection(url) {
                        @Override public void connect() { connected = true; }
                        @Override public InputStream getInputStream() throws IOException {
                            InputStream stream = archive.open(name);
                            if (stream == null) throw new IOException("Payload resource missing: " + name);
                            return stream;
                        }
                    };
                }
            });
        } catch (IOException e) {
            throw new IllegalStateException("Invalid payload resource URL: " + name, e);
        }
    }

    private static boolean isPayloadName(String name) {
        return name.startsWith("modcore.") && !name.startsWith("modcore.agent.");
    }

    private static boolean isPayloadResource(String name) {
        return name.startsWith("modcore/") && !name.startsWith("modcore/agent/");
    }
}
