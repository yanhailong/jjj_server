/**
 * FileHelper.java 2012-4-25
 */
package com.jjg.game.common.utils;

import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * 文件读取与解析帮助者类，用于读取指定文件，解析文件
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

        FileChannel fc = null;
        try {
            fc = new RandomAccessFile(filename, "r").getChannel();
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
            return new byte[0];
        } finally {
            try {
                fc.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 读取指定文件，返回文件内容
     *
     * @param file        需要被读取的文件
     * @param charsetName 读取文件采用的编码
     * @return
     */
    public static String readFile(File file, String charsetName) {
        FileInputStream fin = null;
        InputStreamReader inReader = null;
        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();
        try {
            fin = new FileInputStream(file);
            inReader = new InputStreamReader(fin, charsetName);
            br = new BufferedReader(inReader);
            char[] charBuffer = new char[1024];
            int n = 0;
            while ((n = br.read(charBuffer)) != -1) {
                sb.append(charBuffer, 0, n);
            }
            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fin != null) {
                try {
                    fin.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (inReader != null) {
                try {
                    inReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
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
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file, append);
            fos.write(content.getBytes("UTF-8"));
            fos.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
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
                if (file.getParentFile()!=null) {
                    file.getParentFile().mkdirs();
                }
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        saveFile(file, content, append);
    }
}
