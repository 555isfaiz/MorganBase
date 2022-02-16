/*
 * Usage:
 */
package morgan.support;

import io.netty.handler.codec.http.HttpResponseStatus;

import java.io.*;
import java.net.JarURLConnection;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * <p>
 * 
 * @author Mark D</a>
 * @version 1.0, 2019.4.16
 */
public class Utils {

    private static final Random RANDOM = new Random();

    public static byte[] intToBytes(int data)
    {
        byte[] bytes = new byte[4];
        bytes[3] = (byte) (data & 0xff);
        bytes[2] = (byte) ((data & 0xff00) >> 8);
        bytes[1] = (byte) ((data & 0xff0000) >> 16);
        bytes[0] = (byte) ((data & 0xff000000) >> 24);
        return bytes;
    }

    public static byte[] longToBytes(long data)
    {
        byte[] bytes = new byte[8];
        bytes[7] = (byte) (data & 0xff);
        bytes[6] = (byte) ((data & 0xff00) >> 8);
        bytes[5] = (byte) ((data & 0xff0000) >> 16);
        bytes[4] = (byte) ((data & 0xff000000) >> 24);
        long t = data >> 32;
        bytes[3] = (byte) ((t & 0xff));
        bytes[2] = (byte) ((t & 0xff00) >> 8);
        bytes[1] = (byte) ((t & 0xff0000) >> 16);
        bytes[0] = (byte) ((t & 0xff000000) >> 24);
        return bytes;
    }

    public static int bytesToInt(byte[] bytes){
        if (bytes.length < 4){
            throw new IllegalArgumentException("bytes length of a int must be 4!");
        }

        return (bytes[0] & 0xFF) << 24 | (bytes[1] & 0xFF) << 16 | (bytes[2] & 0xFF) << 8 | bytes[3] & 0xFF;
    }

    public static long bytesToLong(byte[] bytes){
        if (bytes.length < 8){
            throw new IllegalArgumentException("bytes length of a long must be 8!");
        }

        return (((long) bytes[0] & 0xFF) << 56) |
                (((long) bytes[1] & 0xFF) << 48) |
                (((long) bytes[2] & 0xFF) << 40) |
                (((long) bytes[3] & 0xFF) << 32) |
                (((long) bytes[4] & 0xFF) << 24) |
                (((long) bytes[5] & 0xFF) << 16) |
                (((long) bytes[6] & 0xFF) << 8) |
                ((long) bytes[7] & 0xFF);
    }

    public static int nextInt(int min, int max){
        if (min >= max)
            return min;
        return min + RANDOM.nextInt(max - min);
    }

    public static List<Class<?>> getClassFromPackage(String packageName) {
		List<Class<?>> list = new ArrayList<>();
		var classLoader = Thread.currentThread().getContextClassLoader();
		var path = packageName.replace('.', '/');
		try {
			var dirs = classLoader.getResources(path);
			while (dirs.hasMoreElements()) {
				var url = dirs.nextElement();

				if (url.getProtocol().equals("jar")) {
                    JarFile jarFile = null;
                    try{
                        jarFile = ((JarURLConnection) url.openConnection()).getJarFile();
                    } catch(Exception e){
                        e.printStackTrace();
                    }
                    if(jarFile != null) {
                        Enumeration<JarEntry> entries = jarFile.entries();
                        while(entries.hasMoreElements()) {
                            JarEntry jarEntry = entries.nextElement();
                            String name = jarEntry.getName();
                            if(name.startsWith(path) && name.endsWith(".class")) {
                                list.add(classLoader.loadClass(name.replace('/', '.').replace(".class", "")));
                            }
                        }
                    }
                } else if (url.getProtocol().equals("file")) {
                    File d = new File(URLDecoder.decode(url.getFile(), StandardCharsets.UTF_8));
                    var files = d.listFiles(pathname -> pathname.getName().endsWith(".class"));

                    for (var file : files) {
                        list.add(classLoader.loadClass(packageName + "." + file.getName().substring(0, file.getName().length() - 6)));
                    }
                }
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return list;
	}

	public static byte[] readFileFromResource(String fileName) {
        InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName);
        if (inputStream == null) {
            return null;
        }

        byte[] bytes = null;
        try {
            bytes = inputStream.readAllBytes();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bytes;
    }

    public static byte[] readFile(String fileName) {
        byte[] bytes = null;
        try {
            var inputStream = new FileInputStream(fileName);
            bytes = inputStream.readAllBytes();
        } catch (Exception e) {
            Log.common.error("e: {}", e.getMessage());
        }
        return bytes;
    }

    public static boolean writeFileToResource(String resourceFolder, String subFolder, String fileName, byte[] bytes) {
        boolean written = true;
        try {
            var folder = new File(resourceFolder + "/" + subFolder);
            if (!folder.exists() && !folder.mkdirs()) {
                Log.http.error("create folder failed, file name:{}, subFolder:{}", fileName, subFolder);
                return false;
            }

            var f = new File(resourceFolder + "/" + subFolder + "/" + fileName);
            if (f.createNewFile()) {
                OutputStream out = new FileOutputStream(f, false);
                out.write(bytes);
                out.close();
            } else {
                written = false;
                Log.http.error("create file failed, file name:{}, subFolder:{}", fileName, subFolder);
            }

        } catch (Exception e) {
            written = false;
            e.printStackTrace();
        }
        return written;
    }

    public static boolean deleteFile(String fileName) {
        var f = new File(fileName);
        return f.delete();
    }

    public static boolean moveFiles(String from, String to) {
        var toPath = new File(to);
        if (!toPath.exists() && !toPath.mkdirs()) {
            Log.common.error("move file failed, cant create target folder. move to: {}", to);
            return false;
        }

        var fromPath = new File(from);
        if (!fromPath.exists()) {
            Log.common.error("move file failed, from path not exist. move from: {}", fromPath);
            return false;
        }

        var files = fromPath.listFiles();
        if (files != null) {
            for (var f : files) {
                if(f.renameTo(new File(to + f.getName())))
                    return false;
            }
        }
        fromPath.delete();
        return true;
    }

    public static String[] getFiles(String path) {
        var f = new File(path);
        return f.list();
    }
}
