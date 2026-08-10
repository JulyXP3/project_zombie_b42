/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.objectweb.asm.AnnotationVisitor
 *  org.objectweb.asm.ClassReader
 *  org.objectweb.asm.ClassVisitor
 *  org.objectweb.asm.ClassWriter
 *  org.objectweb.asm.MethodVisitor
 *  org.objectweb.asm.tree.AnnotationNode
 *  org.objectweb.asm.tree.ClassNode
 *  org.objectweb.asm.tree.MethodNode
 */
package EtherHack.utils;

import EtherHack.utils.Logger;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class Patch {
    private static final Map<String, ClassNode> classNodeMap = new HashMap<String, ClassNode>();
    private static String projectZomboidJarPath = null;

    public static void setProjectZomboidJarPath(String jarPath) {
        projectZomboidJarPath = jarPath;
        Logger.printLog("Set ProjectZomboid.jar path: " + jarPath);
    }

    public static void injectIntoClass(String className, String methodName, boolean isStatic, Consumer<MethodNode> injector) {
        Logger.print("Injection into a game file '" + className + "' in method: '" + methodName + "'");
        ClassNode classNode = classNodeMap.computeIfAbsent(className, Patch::loadClassNode);
        if (classNode == null) {
            throw new RuntimeException("Failed to load class " + className);
        }
        for (MethodNode methodNode : classNode.methods) {
            if (!methodNode.name.equals(methodName) || Modifier.isStatic(methodNode.access) != isStatic) continue;
            if (!Patch.hasInjectedAnnotation(methodNode)) {
                Patch.addInjectAnnotation(classNode, methodName);
            }
            injector.accept(methodNode);
        }
        classNodeMap.put(className, classNode);
    }

    public static void modifyClass(String className, Consumer<ClassNode> modifier) {
        ClassNode classNode = classNodeMap.computeIfAbsent(className, Patch::loadClassNode);
        if (classNode == null) {
            throw new RuntimeException("Failed to load class " + className);
        }
        modifier.accept(classNode);
        classNodeMap.put(className, classNode);
    }

    private static ClassNode loadClassNode(String key) {
        ClassNode node = new ClassNode();
        try {
            if (projectZomboidJarPath != null && Files.exists(Paths.get(projectZomboidJarPath, new String[0]), new LinkOption[0])) {
                try (JarFile jarFile = new JarFile(projectZomboidJarPath)) {
                    ZipEntry entry = jarFile.getEntry(key + ".class");
                    if (entry != null) {
                        try (InputStream is = jarFile.getInputStream(entry)) {
                            ClassReader reader = new ClassReader(is);
                            reader.accept((ClassVisitor)node, 8);
                            Logger.printLog("Loaded class from ProjectZomboid.jar: " + key);
                            return node;
                        }
                    }
                }
            }
            ClassReader reader = new ClassReader(key);
            reader.accept((ClassVisitor)node, 8);
            Logger.printLog("Loaded class from file system: " + key);
            return node;
        }
        catch (IOException e) {
            Logger.error("Failed to read class: " + key, e);
            return null;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static boolean isInjectedAnnotationPresent(String file, String baseDir) {
        boolean result;
        Path filePath = Paths.get(baseDir, file);
        InputStream inputStream = null;
        ZipFile jarFile = null;
        try {
            ZipEntry entry;
            if (Files.exists(filePath, new LinkOption[0])) {
                inputStream = new FileInputStream(filePath.toString());
            } else if (projectZomboidJarPath != null && Files.exists(Paths.get(projectZomboidJarPath, new String[0]), new LinkOption[0]) && (entry = ((JarFile)(jarFile = new JarFile(projectZomboidJarPath))).getEntry(file)) != null) {
                inputStream = ((JarFile)jarFile).getInputStream(entry);
            }
            if (inputStream == null) {
                Logger.printLog("Could not find class file: " + file);
                return false;
            }
            ClassReader reader = new ClassReader(inputStream);
            final boolean[] found = new boolean[]{false};
            reader.accept(new ClassVisitor(589824){

                public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                    MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                    return new MethodVisitor(589824, mv){

                        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                            if (descriptor.equals("LEtherHack/annotations/Injected;")) {
                                found[0] = true;
                            }
                            return super.visitAnnotation(descriptor, visible);
                        }
                    };
                }
            }, 0);
            result = found[0];
        }
        catch (IOException e) {
            Logger.error("Error checking for injected annotations", e);
            return false;
        }
        finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                }
                catch (IOException iOException) {}
            }
            if (jarFile != null) {
                try {
                    jarFile.close();
                }
                catch (IOException iOException) {}
            }
        }
        return result;
    }

    private static void addInjectAnnotation(ClassNode classNode, String methodName) {
        for (MethodNode method : classNode.methods) {
            boolean hasAnnotation;
            if (!method.name.equals(methodName)) continue;
            if (method.visibleAnnotations == null) {
                method.visibleAnnotations = new LinkedList();
            }
            if (!(hasAnnotation = method.visibleAnnotations.stream().anyMatch(anno -> anno.desc.equals("LEtherHack/annotations/Injected;")))) {
                method.visibleAnnotations.add(new AnnotationNode("LEtherHack/annotations/Injected;"));
            }
            return;
        }
    }

    private static boolean hasInjectedAnnotation(MethodNode method) {
        if (method.visibleAnnotations == null) {
            return false;
        }
        return method.visibleAnnotations.stream().anyMatch(anno -> anno.desc.equals("LEtherHack/annotations/Injected;"));
    }

    public static void saveModifiedClasses() {
        for (Map.Entry<String, ClassNode> entry : classNodeMap.entrySet()) {
            String className = entry.getKey();
            ClassNode classNode = entry.getValue();
            try {
                SafeClassWriter writer = new SafeClassWriter(2);
                classNode.accept((ClassVisitor)writer);
                byte[] bytes = writer.toByteArray();
                Path classFilePath = Paths.get(className + ".class", new String[0]);
                if (classFilePath.getParent() != null) {
                    Files.createDirectories(classFilePath.getParent(), new FileAttribute[0]);
                }
                try (FileOutputStream fos = new FileOutputStream(classFilePath.toFile());){
                    fos.write(bytes);
                }
                Logger.printLog("Successfully saved modified class: " + className);
            }
            catch (IOException e) {
                Logger.error("Error saving modified class '" + className + "'", e);
            }
            catch (Exception e) {
                Logger.error("Unexpected error processing class '" + className + "'", e);
            }
        }
    }

    private static class SafeClassWriter
    extends ClassWriter {
        public SafeClassWriter(int flags) {
            super(flags);
        }

        protected String getCommonSuperClass(String type1, String type2) {
            try {
                return super.getCommonSuperClass(type1, type2);
            }
            catch (RuntimeException e) {
                Logger.printLog("Could not resolve common superclass for " + type1 + " and " + type2 + ", using Object");
                return "java/lang/Object";
            }
        }
    }
}
