package modcore.build;

import modcore.GamePatcher;
import modcore.agent.PayloadArchive;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

/** Offline preparation only: original JAR input is read-only, output is an explicit deployment file. */
public final class AgentLoadout {
    private AgentLoadout() {}

    public static void prepare(Path gameJar, Path runtimeJar, Path destination) throws IOException {
        prepare(gameJar, runtimeJar, destination, false);
    }

    public static void prepareRuntime(Path gameJar, Path runtimeJar, Path destination) throws IOException {
        prepare(gameJar, runtimeJar, destination, true);
    }

    private static void prepare(Path gameJar, Path runtimeJar, Path destination, boolean plain) throws IOException {
        Path game = gameJar.toRealPath();
        Path runtime = runtimeJar.toRealPath();
        Path target = destination.toAbsolutePath().normalize();
        if (target.equals(game) || target.equals(runtime) || target.startsWith(game.getParent())) {
            throw new IOException("Payload output must be separate from original game files");
        }
        String gameDigest = PayloadArchive.digest(game);
        GamePatcher.PreparedPatches prepared = new GamePatcher().prepareAgentPatches(game);
        Properties metadata = new Properties();
        metadata.setProperty("format.version", plain ? "2" : "1");
        metadata.setProperty("main.class", "zombie.gameStates.MainScreenState");
        metadata.setProperty("game.sha256", gameDigest);
        metadata.setProperty("disabled.patches", String.join(",", prepared.unavailable()));
        ByteArrayOutputStream archive = new ByteArrayOutputStream();
        try (JarOutputStream out = new JarOutputStream(archive);
             JarFile source = new JarFile(runtime.toFile(), true);
             JarFile original = new JarFile(game.toFile(), true)) {
            for (JarEntry entry : source.stream().filter(entry -> !entry.isDirectory()).toList()) {
                String name = entry.getName();
                if (name.equals("META-INF/MANIFEST.MF")) continue;
                if (!name.startsWith("modcore/") || name.startsWith("modcore/agent/")
                        || name.startsWith("modcore/build/") || name.endsWith("coreboot.class")) {
                    throw new IOException("Unexpected runtime archive entry: " + name);
                }
                try (InputStream in = source.getInputStream(entry)) {
                    byte[] bytes = in.readAllBytes();
                    if (plain) metadata.setProperty("entry." + name, sha256(bytes));
                    put(out, name, bytes);
                }
            }
            for (Map.Entry<String, byte[]> patch : prepared.classes().entrySet()) {
                JarEntry entry = original.getJarEntry(patch.getKey() + ".class");
                if (entry == null) throw new IOException("Original class missing: " + patch.getKey());
                try (InputStream in = original.getInputStream(entry)) {
                    metadata.setProperty("original." + patch.getKey(), sha256(in.readAllBytes()));
                }
                String name = PayloadArchive.PATCH_ROOT + patch.getKey() + ".class";
                if (plain) metadata.setProperty("entry." + name, sha256(patch.getValue()));
                put(out, name, patch.getValue());
            }
            ByteArrayOutputStream config = new ByteArrayOutputStream();
            metadata.store(config, "Instrumentation deployment metadata");
            put(out, PayloadArchive.METADATA, config.toByteArray());
        }
        if (!gameDigest.equals(PayloadArchive.digest(game))) {
            throw new IOException("Game JAR changed during preparation");
        }
        byte[] envelope = plain ? archive.toByteArray() : encrypt(archive.toByteArray());
        Files.createDirectories(target.getParent());
        Path staging = Files.createTempFile(target.getParent(), ".modcore-stage-", ".bin");
        try {
            Files.write(staging, envelope);
            if (plain) PayloadArchive.readRuntime(staging);
            else PayloadArchive.read(staging);
            try {
                Files.move(staging, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                throw new IOException("Atomic payload replacement is not supported at " + target, e);
            }
        } finally {
            Files.deleteIfExists(staging);
        }
        System.out.println("Prepared " + prepared.classes().size() + " class patches: " + target);
        if (!prepared.unavailable().isEmpty()) {
            System.err.println("Known unavailable preview patches: " + prepared.unavailable());
        }
    }

    private static byte[] encrypt(byte[] plain) throws IOException {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(key);
            out.write(cipher.getIV());
            out.write(cipher.doFinal(plain));
            return out.toByteArray();
        } catch (GeneralSecurityException e) {
            throw new IOException("Payload encryption failed", e);
        } finally {
            java.util.Arrays.fill(key, (byte) 0);
            java.util.Arrays.fill(plain, (byte) 0);
        }
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void put(JarOutputStream out, String name, byte[] bytes) throws IOException {
        out.putNextEntry(new JarEntry(name));
        out.write(bytes);
        out.closeEntry();
    }
}
