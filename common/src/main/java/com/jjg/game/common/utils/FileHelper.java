/**
 * FileHelper.java 2012-4-25
 */
package com.jjg.game.common.utils;

import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * 文件读取与解析帮助者类，用于读取指定文件，解析文件
 *
 * @version 1.0
 * @All rights reserved.
 */
public class FileHelper {
    /**
     * Mapped File way MappedByteBuffer 可以在处理大文件时，提升性能
     *
     * @param filename
     * @return
     * @throws IOException
     */
    public static byte[] read(String filename) {
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(filename, "r")) {
            FileChannel fc = randomAccessFile.getChannel();
            MappedByteBuffer byteBuffer = fc.map(FileChannel.MapMode.READ_ONLY, 0,
                    fc.size()).load();
            System.out.println(byteBuffer.isLoaded());
            byte[] result = new byte[(int) fc.size()];
            if (byteBuffer.remaining() > 0) {
                // System.out.println("remain");
                byteBuffer.get(result, 0, byteBuffer.remaining());
            }
            return result;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new byte[0];

    }

    /**
     * 读取指定文件，返回文件内容
     *
     * @param file        需要被读取的文件
     * @param charsetName 读取文件采用的编码
     * @return
     */
    public static String readFile(File file, String charsetName) {
        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();
        try (FileInputStream fin = new FileInputStream(file); InputStreamReader inReader = new InputStreamReader(fin, charsetName)) {
            br = new BufferedReader(inReader);
            char[] charBuffer = new char[1024];
            int n;
            while ((n = br.read(charBuffer)) != -1) {
                sb.append(charBuffer, 0, n);
            }
            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 保存指定的内容到文件中
     *
     * @param file    用于保存的文件
     * @param content 内容
     * @param append  是否添加到后面
     */
    public static void saveFile(File file, String content, boolean append) {
        try (FileOutputStream fos = new FileOutputStream(file, append)) {
            fos.write(content.getBytes("UTF-8"));
            fos.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 保存指定的内容到文件中
     *
     * @param fileName 用于保存的文件名称
     * @param content  内容
     * @param append   是否添加到后面
     */
    public static void saveFile(String fileName, String content,
                                boolean append) {
        File file = new File(fileName);
        if (!file.exists()) {
            try {
                if (file.getParentFile() != null) {
                    if (!file.getParentFile().exists()) {
                        boolean mkDirs = file.getParentFile().mkdirs();
                        if (!mkDirs) {
                            throw new RuntimeException("创建文件夹失败");
                        }
                    }
                }
                boolean newFile = file.createNewFile();
                if (!newFile) {
                    throw new RuntimeException("创建文件失败");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        saveFile(file, content, append);
    }
}
