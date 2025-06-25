package com.jjg.game.utils;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.ExecutorService;

public class FileEx {

  public static final RandomAccessFile openRandomAccessFile(File f, String mode)
      throws FileNotFoundException {
    return new RandomAccessFile(f, mode);
  }

  public static final RandomAccessFile openRandomAccessFile(String f, String mode)
      throws FileNotFoundException {
    return new RandomAccessFile(f, mode);
  }

  public static final InputStream openInputStreamByUrl(String url)
      throws MalformedURLException, IOException {
    URL u = new URL(url);
    InputStream in = openInputStreamByUrl(u);
    return in;
  }

  public static final InputStream openInputStreamByUrl(URL url) throws IOException {
    InputStream in = url.openStream();
    return in;
  }

  public static final byte[] readFully(URL url) throws MalformedURLException, IOException {
    InputStream in = openInputStreamByUrl(url);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    byte[] buff = null;
    int len = 0;
    do {
      buff = new byte[8 * 1024];
      len = in.read(buff);
      if (len < 0) {
        break;
      }
      baos.write(buff, 0, len);
    } while (len > 0);

    return baos.toByteArray();
  }

  public static final int getLength(String file) {
    File f = openFile(file);
    return (int) f.length();
  }

  /**
   * 取得目录下的所有文件名(包含所有子文件夹)
   *
   * @param path
   * @return
   * @throws IOException
   */
  public static List<String> getFile(String path, boolean isAbsolutePath) throws IOException {
    return getFile(path, null, isAbsolutePath);
  }

  /**
   * 取得目录下的所有文件名(包含所有子文件夹,但不包含目录名)
   *
   * @param path
   * @return
   * @throws IOException
   */
  public static List<String> getFile(String path) throws IOException {
    return getFile(path, null, false);
  }

  /**
   * 取得目录下的所有文件名(包含所有子文件夹,但不包含目录名) 注意:文件夹不参与正则
   *
   * @param path
   * @return
   * @throws IOException
   */
  public static List<String> getFile(String path, String regex, boolean isAbsolutePath)
      throws IOException {
    List<String> list = new ArrayList<String>();
    File file = new File(path);
    File[] array = file.listFiles();
    if (array == null || array.length == 0) {
      return list;
    }
    for (int i = 0; i < array.length; i++) {
      if (array[i].isDirectory()) {
        list.addAll(getFile(array[i].getPath(), regex, isAbsolutePath));
      } else {
        if (!StrEx.isEmpty(regex)) {
          if (!array[i].getName().matches(regex)) {
            continue;
          }
        }
        String localPath = "";
        if (isAbsolutePath) {
          localPath = array[i].getAbsolutePath();
        } else {
          localPath = array[i].getName();
        }
        list.add(localPath);
      }
    }

    return list;
  }

  /**
   * 取得第2个参数目录下的所有文件名 文件名的表示方法相对于指定父节点(参数1)的路径
   *
   * @param pPath 父节点
   * @param cPath 子节点
   * @return
   * @throws Exception
   */
  public static List<String> getFileRelativeName(String pPath, String cPath) throws Exception {
    List<String> list = new ArrayList<String>();
    File file = new File(cPath);
    File[] array = file.listFiles();
    for (int i = 0; i < array.length; i++) {
      if (array[i].isDirectory()) {
        list.addAll(getFileRelativeName(pPath, array[i].getPath()));
      } else {
        String filePath = array[i].getPath();
        filePath = filePath.replace("\\", "/");
        int endIndex = filePath.lastIndexOf(pPath);
        if (endIndex == -1) {
          throw new Exception("在当前文件路径中没有找到字符串父节点.父节点:" + pPath + "子节点:" + cPath);
        }
        list.add(filePath.substring(endIndex + pPath.length()));
      }
    }
    return list;
  }

  /**
   * 取得目录下的目录名
   *
   * @param path
   * @return
   * @throws IOException
   */
  public static List<File> getDirectory(String path) throws IOException {
    List<File> list = new ArrayList<File>();
    // get file list where the path has
    File file = new File(path);
    // get the folder list
    File[] array = file.listFiles();
    for (int i = 0; i < array.length; i++) {
      // System.out.println(array[i].getName());
      if (array[i].isDirectory()) {
        list.add(array[i]);
        list.addAll(getDirectory(array[i].getPath()));
      }
    }
    return list;
  }

  /**
   * 创建目录 createFolder
   *
   * @param path
   * @return
   * @throws Exception
   */
  public static final boolean createFolderByFile(String path) {
    File file = new File(path);
    File parent = file.getParentFile();
    return createFolder(parent);
  }

  /**
   * 创建目录 createFolder
   *
   * @param path
   * @return
   * @throws Exception
   */
  public static final boolean createFolder(File file) {
    if (!file.exists()) {
      return file.mkdirs();
    }
    return false;
  }

  /**
   * 创建目录 createFolder
   *
   * @param path
   * @return
   * @throws Exception
   */
  public static final boolean createFolder(String path) {
    File file = new File(path);
    if (!file.exists()) {
      return file.mkdirs();
    }
    return false;
  }

  /** 检查文件是否存在 exists */
  public static final boolean isExists(String path) {
    File file = new File(path);
    boolean b = file.exists();
    file = null;
    return b;
  }

  /**
   * 新建文件
   *
   * @param file String 文件路径及名称 如c:/fqf.txt
   * @param fileContent String 文件内容
   * @return boolean
   */
  public static final boolean createFile(String filePath, String fileContent) {
    try {
      File myFilePath = new File(filePath);
      if (!myFilePath.exists()) {
        FileEx.createFolderByFile(filePath);
        myFilePath.createNewFile();
      }
      if (fileContent == null) {
        FileWriter resultFile = new FileWriter(myFilePath);
        PrintWriter myFile = new PrintWriter(resultFile);
        myFile.println(fileContent);
        myFile = null;
        resultFile.close();
        resultFile = null;
      }

      myFilePath = null;
    } catch (Exception e) {
      LoggerUtils.LOGGER.error(ExceptionEx.e2s(e));
      return false;
    }
    return true;
  }

  /**
   * 删除文件
   *
   * @param file String 文件路径及名称 如c:/fqf.txt
   * @param fileContent String
   * @return boolean
   */
  public static final void delFile(String file) {
    try {
      String filePath = file;
      filePath = filePath.toString();
      java.io.File myDelFile = new java.io.File(filePath);
      myDelFile.delete();
      myDelFile = null;

    } catch (Exception e) {
      LoggerUtils.LOGGER.error(ExceptionEx.e2s(e));
    }
  }

  /**
   * 删除文件夹
   *
   * @param isDeleteOldPath
   * @param filePathAndName String 文件夹路径及名称 如c:/fqf
   * @param fileContent String
   * @return boolean
   */
  public static final void delFolder(String folderPath, boolean isDeleteOldPath) {
    // 删除完里面所有内容
    delAllFile(folderPath);
    String filePath = folderPath;
    filePath = filePath.toString();
    if (isDeleteOldPath) {
      java.io.File myFilePath = new java.io.File(filePath);
      // 删除空文件夹
      myFilePath.delete();
      myFilePath = null;
    }
  }

  /**
   * 删除文件夹里面的所有文件
   *
   * @param path String 文件夹路径 如 c:/fqf
   */
  public static final void delAllFile(String path) {
    File file = new File(path);
    if (!file.exists()) {
      file = null;
      return;
    }
    if (!file.isDirectory()) {
      file = null;
      return;
    }
    String[] tempList = file.list();
    File temp = null;
    for (int i = 0; i < tempList.length; i++) {
      if (path.endsWith(File.separator)) {
        temp = new File(path + tempList[i]);
      } else {
        temp = new File(path + File.separator + tempList[i]);
      }
      if (temp.isFile()) {
        temp.delete();
      }
      if (temp.isDirectory()) {
        // delAllFile(path + "/" + tempList[i]); // 先删除文件夹里面的文件
        // delFolder(path + "/" + tempList[i], true); // 再删除空文件夹
        delFolder(path + "/" + tempList[i], true);
      }
    }
    temp = null;
    tempList = null;
  }

  /**
   * 复制单个文件
   *
   * @param oldPath String 原文件路径 如：c:/fqf.txt
   * @param newPath String 复制后路径 如：f:/fqf.txt
   * @return boolean
   * @throws IOException
   */
  public static final void copyFile(String oldPath, String newPath) throws IOException {
    File newfile = new File(newPath);
    if (!newfile.exists()) {
      createFolderByFile(newPath);
    }

    BufferedInputStream input = new BufferedInputStream(new FileInputStream(oldPath));
    BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(newPath));
    byte[] b = new byte[1024 * 5];
    int len;
    while ((len = input.read(b)) != -1) {
      output.write(b, 0, len);
    }
    output.flush();
    output.close();
    input.close();
    input = null;
    output = null;
  }

  /**
   * 复制单个文件
   *
   * @param oldPath String 原文件路径 如：c:/fqf.txt
   * @param newPath String 复制后路径 如：f:/fqf.txt
   * @return boolean
   * @throws IOException
   */
  public static final void copyFileByAllocateDirect(String oldPath, String newPath)
      throws IOException {
    File newfile = new File(newPath);
    if (!newfile.exists()) {
      createFolderByFile(newPath);
    }

    @SuppressWarnings("resource")
    FileChannel inChanel = new FileInputStream(oldPath).getChannel();
    @SuppressWarnings("resource")
    FileChannel outChannel = new FileOutputStream(newPath).getChannel();
    java.nio.ByteBuffer byteBuffer = java.nio.ByteBuffer.allocateDirect(1024 * 5);
    while (true) {
      byteBuffer.clear();
      int b = inChanel.read(byteBuffer);
      if (b == -1) {
        break;
      }
      byteBuffer.flip();
      outChannel.write(byteBuffer);
    }
    byteBuffer = null;
    inChanel.close();
    outChannel.close();
    inChanel = null;
    outChannel = null;
  }

  /**
   * 复制整个文件夹内容
   *
   * @param oldPath String 原文件路径 如：c:/fqf
   * @param newPath String 复制后路径 如：f:/fqf/ff
   * @param isDeleteOldPath2
   * @param isDeleteOldPath
   * @return boolean
   * @throws Exception
   */
  public static void copyFolder(
      final String oldPath,
      final String newPath,
      final boolean isDeleteOldFile,
      boolean isDeleteOldPath,
      final ExecutorService exceutor)
      throws Exception {
    // 如果文件夹不存在 则建立新文件夹
    (new File(newPath)).mkdirs();
    File oldPathFile = new File(oldPath);
    String[] files = oldPathFile.list();
    for (int i = 0; i < files.length; i++) {
      final String fileName = files[i];
      if (exceutor == null) {
        copyFolder(oldPath, newPath, fileName, isDeleteOldFile);
      } else {
        exceutor.execute(
            new Runnable() {
              @Override
              public void run() {
                try {
                  copyFolder(oldPath, newPath, fileName, isDeleteOldFile);
                } catch (Exception e) {
                  LoggerUtils.LOGGER.error(ExceptionEx.e2s(e));
                }
              }
            });
      }
    }

    if (isDeleteOldPath) {
      delFolder(oldPath, isDeleteOldPath);
    }
  }

  private static void copyFolder(
      String oldPath, String newPath, String fileName, boolean isDeleteOldFile) throws Exception {
    File temp = null;
    if (oldPath.endsWith(File.separator)) {
      temp = new File(oldPath + fileName);
    } else {
      temp = new File(oldPath + File.separator + fileName);
    }
    if (temp.isFile()) {
      String newoldPath;
      if (oldPath.endsWith(File.separator)) {
        newoldPath = oldPath + fileName;
      } else {
        newoldPath = oldPath + File.separator + fileName;
      }
      copyFileByAllocateDirect(newoldPath, newPath + "/" + (temp.getName()).toString());
      delFile(newoldPath);
    } else if (temp.isDirectory()) {
      // 子文件夹
      // 传递线程池,线程错误
      copyFolder(oldPath + "/" + fileName, newPath + "/" + fileName, isDeleteOldFile, true, null);
    }
  }

  /**
   * 移动文件到指定目录
   *
   * @param oldPath String 如：c:/fqf.txt
   * @param newPath String 如：d:/fqf.txt
   * @throws IOException
   */
  public static final void moveFile(String oldPath, String newPath) throws IOException {
    copyFileByAllocateDirect(oldPath, newPath);
    delFile(oldPath);
  }

  /**
   * 移动文件到指定目录
   *
   * @param oldPath String 如：c:/fqf.txt
   * @param newPath String 如：d:/fqf.txt
   * @throws Exception
   */
  public static final void moveFolder(
      String oldPath,
      String newPath,
      boolean isDeleteOldFile,
      boolean isDeleteOldPath,
      ExecutorService exceutor)
      throws Exception {
    copyFolder(oldPath, newPath, isDeleteOldFile, isDeleteOldPath, exceutor);
  }

  public static final boolean rename(String srcPath, String name) {
    File file = new File(srcPath);
    File newFile = new File(file.getParent() + "/" + name);
    boolean b = file.renameTo(newFile);
    file = null;
    newFile = null;
    return b;
  }

  /**
   * 判断是否是目录
   *
   * @param path
   * @return
   */
  public static final boolean isDir(String path) {
    File file = new File(path);
    boolean b = file.isDirectory();
    file = null;
    return b;
  }

  /**
   * 判断是否是文件
   *
   * @param path
   * @return
   */
  public static final boolean isFile(String path) {
    File file = new File(path);
    boolean b = file.isFile();
    file = null;
    return b;
  }

  /**
   * read
   *
   * @param file
   * @param skip
   * @param len
   * @return
   * @throws IOException
   * @throws Exception
   */
  public static final byte[] read(String file, long skip, int len) throws IOException {
    InputStream input = openInputStream(file);
    byte[] data = new byte[len];
    input.skip(skip);
    int readlen = input.read(data);
    input.close();
    input = null;
    if (len == readlen) {
      return data;
    } else {
      byte[] readData = new byte[readlen];
      System.arraycopy(data, 0, readData, 0, readlen);
      return readData;
    }
  }

  public static final byte[] readFully(File f) {
    byte[] b = null;

    if (f == null) {
      return b;
    }
    try {
      int len = (int) f.length();
      b = new byte[len];
      DataInputStream dis = new DataInputStream(new FileInputStream(f));
      dis.readFully(b);
      dis.close();
      return b;
    } catch (IOException e) {
      LoggerUtils.LOGGER.error(ExceptionEx.e2s(e));
    }
    return null;
  }

  public static final String readFully(File f, String charset) throws IOException {
    byte[] b = readFully(f);
    return new String(b, charset);
  }

  public static final byte[] readFully(InputStream in) {
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    if (in == null) {
      return null;
    }
    try {
      int len = 0;
      do {
        byte[] buff = new byte[8 * 1024];
        len = in.read(buff);
        if (len < 0) {
          break;
        }

        buf.write(buff, 0, len);
      } while (len > 0);
    } catch (IOException e) {
      LoggerUtils.LOGGER.error(ExceptionEx.e2s(e));
    }

    return buf.toByteArray();
  }

  public static final byte[] readFully(String file) throws IOException {
    File f = new File(file);
    DataInputStream dis = new DataInputStream(new FileInputStream(f));
    byte[] data = new byte[(int) f.length()];
    dis.readFully(data);
    dis.close();
    f = null;
    dis = null;
    return data;
  }

  public static final Properties readProperties(File f) {
    try {
      if (f == null || !f.exists()) {
        return null;
      }
      Properties p = new Properties();
      FileInputStream fis;
      fis = new FileInputStream(f);
      p.load(fis);
      fis.close();
      return p;
    } catch (Exception e) {
      LoggerUtils.LOGGER.error(ExceptionEx.e2s(e));
    }
    return null;
  }

  public static final String readAll(String file) throws IOException {
    return readAll(new File(file));
  }

  public static final String readAll(InputStream in) throws IOException {
    InputStreamReader isr = new InputStreamReader(in, Charset.forName("UTF-8"));
    BufferedReader br = openBufferedReader(isr);
    String line = "";
    // StringBuffer sb = StringBufPool.borrowObject();
    StringBuffer sb = new StringBuffer();
    // try {
    while ((line = br.readLine()) != null) {
      sb.append(line);
      sb.append("\r\n");
    }
    br.close();

    br = null;
    return sb.toString();
    // } finally {
    // StringBufPool.returnObject(sb);
    // }
  }

  public static final String readAll(File file) throws IOException {
    InputStreamReader isr =
        new InputStreamReader(new FileInputStream(file), Charset.forName("UTF-8"));
    BufferedReader br = openBufferedReader(isr);
    String line = "";
    // StringBuffer sb = StringBufPool.borrowObject();
    StringBuffer sb = new StringBuffer();
    // try {
    while ((line = br.readLine()) != null) {
      sb.append(line);
      sb.append("\r\n");
    }
    br.close();

    br = null;
    return sb.toString();
    // } finally {
    // StringBufPool.returnObject(sb);
    // }
  }

  public static final List<String> readLines(String file) throws IOException {
    return readLines(new File(file));
  }

  public static final List<String> readLines(File file) throws IOException {
    BufferedReader br = openBufferedReader(file);
    List<String> ret = new Vector<String>();
    int times = 100000;
    while (true) {
      if (times-- <= 0) {
        break;
      }
      String line = br.readLine();
      if (line == null) {
        break;
      }
      ret.add(line);
    }
    br.close();
    br = null;
    return ret;
  }

  public static final void write(String file, boolean append, byte[] data) throws IOException {
    File f = new File(file);
    BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(f, append));
    output.write(data);
    output.flush();
    output.close();
    f = null;
    output = null;
  }

  public static final void write(String file, byte[] data) throws IOException {
    boolean append = false;
    write(file, append, data);
  }

  public static final void writeAll(String file, String str) throws IOException {
    boolean append = false;
    writeAll(file, append, str);
  }

  public static final void writeAll(String file, boolean append, String str) throws IOException {
    File f = new File(file);
    writeAll(f, append, str);
    f = null;
  }

  public static final void writeAll(File file, boolean append, String str) throws IOException {
    BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(file, append));
    BufferedWriter bos =
        new BufferedWriter(new OutputStreamWriter(output, Charset.forName(StrEx.CHARSET_UTF8)));
    // 每次调用 write() 方法都会导致在给定字符(或字符集)上调用编码转换器
    bos.write(str);
    bos.flush();
    bos.close();
    output.close();

    output = null;
  }

  public static final void writeFully(File f, byte[] b) throws IOException {
    writeFully(f, b, false);
  }

  public static final void writeFully(String filename, byte[] b) throws IOException {
    writeFully(filename, b, false);
  }

  public static final void writeFully(String filename, byte[] b, boolean append)
      throws IOException {
    File f = new File(filename);
    writeFully(f, b, append);
  }

  public static final void writeFully(File f, byte[] b, boolean append) throws IOException {
    BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(f, append));
    output.write(b);
    output.flush();
    output.close();
    f = null;
    output = null;
  }

  public static final InputStream openInputStream(String file) throws FileNotFoundException {
    FileInputStream fis = new FileInputStream(file);
    return fis;
  }

  public static final BufferedInputStream openBufferedInputStream(InputStream is) {
    return new BufferedInputStream(is);
  }

  public static final BufferedInputStream openBufferedInputStream(String file)
      throws FileNotFoundException {
    InputStream is = openInputStream(file);
    return openBufferedInputStream(is);
  }

  public static final DataInputStream openDataInputStream(InputStream is)
      throws FileNotFoundException {
    BufferedInputStream bis = openBufferedInputStream(is);
    return new DataInputStream(bis);
  }

  public static final DataInputStream openDataInputStream(String file)
      throws FileNotFoundException {
    InputStream is = openInputStream(file);
    return openDataInputStream(is);
  }

  public static final OutputStream openOutputStream(String file, boolean append)
      throws FileNotFoundException {
    FileOutputStream fos = new FileOutputStream(file, append);
    return fos;
  }

  public static final BufferedOutputStream openBufferedOutputStream(OutputStream os) {
    return new BufferedOutputStream(os);
  }

  public static final OutputStream openOutputStream(String file) throws FileNotFoundException {
    boolean append = false;
    return openOutputStream(file, append);
  }

  public static final DataOutputStream openDataOutputStream(OutputStream os) {
    BufferedOutputStream bos = openBufferedOutputStream(os);
    return new DataOutputStream(bos);
  }

  public static final DataOutputStream openDataOutputStream(String file, boolean append)
      throws FileNotFoundException {
    OutputStream os = openOutputStream(file, append);
    return openDataOutputStream(os);
  }

  public static final DataOutputStream openDataOutputStream(String file)
      throws FileNotFoundException {
    boolean append = false;
    return openDataOutputStream(file, append);
  }

  public static final File openFile(String file) {
    return new File(file);
  }

  public static final InputStream openInputStream(File file) throws FileNotFoundException {
    FileInputStream fis = new FileInputStream(file);
    return fis;
  }

  public static final DataInputStream openDataInputStream(File file) throws FileNotFoundException {
    InputStream is = openInputStream(file);
    return openDataInputStream(is);
  }

  public static final FileWriter openFileWriter(File file) throws IOException {
    return new FileWriter(file);
  }

  public static final FileReader openFileReader(File file) throws FileNotFoundException {
    return new FileReader(file);
  }

  public static final BufferedReader openBufferedReader(Reader reader) {
    return new BufferedReader(reader);
  }

  public static final FileReader openFileReader(String file) throws FileNotFoundException {
    File f = openFile(file);
    return openFileReader(f);
  }

  public static final BufferedReader openBufferedReader(File file) throws FileNotFoundException {
    FileReader fr = openFileReader(file);
    return openBufferedReader(fr);
  }

  public static final BufferedReader openBufferedReader(String file) throws FileNotFoundException {
    FileReader fr = openFileReader(file);
    return openBufferedReader(fr);
  }

  public static final InputStreamReader openInputStreamReader(InputStream is) {
    return new InputStreamReader(is);
  }

  public static final FileWriter openFileWriter(String file) throws IOException {
    File f = openFile(file);
    return openFileWriter(f);
  }

  public static final BufferedWriter openBufferedWriter(Writer writer) {
    return new BufferedWriter(writer);
  }

  public static final BufferedWriter openBufferedWriter(String file) throws IOException {
    FileWriter fw = openFileWriter(file);
    return openBufferedWriter(fw);
  }

  public static final String getPath(String filename) {
    File f = new File(filename);
    return f.getParentFile().getPath();
  }

  public static final String getName(String filename) {
    File f = new File(filename);
    return f.getName();
  }

  public static final String getExtension(String filename) {
    if (filename == null) {
      return null;
    }
    int index = indexOfExtension(filename);
    if (index == -1) {
      return "";
    } else {
      return filename.substring(index + 1);
    }
  }

  public static final char EXTENSION_SEPARATOR = '.';
  private static final char UNIX_SEPARATOR = '/';
  private static final char WINDOWS_SEPARATOR = '\\';

  public static final int indexOfExtension(String filename) {
    if (filename == null) {
      return -1;
    }
    int extensionPos = filename.lastIndexOf(EXTENSION_SEPARATOR);
    int lastSeparator = indexOfLastSeparator(filename);
    return (lastSeparator > extensionPos ? -1 : extensionPos);
  }

  public static final int indexOfLastSeparator(String filename) {
    if (filename == null) {
      return -1;
    }
    int lastUnixPos = filename.lastIndexOf(UNIX_SEPARATOR);
    int lastWindowsPos = filename.lastIndexOf(WINDOWS_SEPARATOR);
    return Math.max(lastUnixPos, lastWindowsPos);
  }

  public static final String getAppRoot() {
    return System.getProperty("user.dir");
  }

  public static boolean compareFileBytes(String file1, String file2) {
    try {
      if (!isExists(file1) || !isExists(file2)) {
        return false;
      }

      boolean isSame = false;
      BufferedInputStream inFile1 = new BufferedInputStream(new FileInputStream(file1));
      BufferedInputStream inFile2 = new BufferedInputStream(new FileInputStream(file2));

      // long startTime = System.currentTimeMillis();
      // 比较文件的长度是否一样
      if (inFile1.available() == inFile2.available()) {
        while (inFile1.read() != -1 && inFile2.read() != -1) {
          if (inFile1.read() != inFile2.read()) {
            // System.out.println("Files not same");
            isSame = false;
            break;
          }
        }
        // System.out.println("two files are same !");
        isSame = true;
      } else {
        isSame = false;
        // System.out.println("two files are different !");
      }

      inFile1.close();
      inFile2.close();

      return isSame;
      // System.out.println("Time Consumed: " +
      // (System.currentTimeMillis() - startTime) + "ms");

    } catch (IOException e) {
      LoggerUtils.LOGGER.error(ExceptionEx.e2s(e));
      return false;
    }
  }

  public static boolean compareFileMd5(String file1, String file2) {
    try {
      if (!isExists(file1) || !isExists(file2)) {
        return false;
      }

      boolean isSame = false;
      BufferedInputStream inFile1 = new BufferedInputStream(new FileInputStream(file1));
      BufferedInputStream inFile2 = new BufferedInputStream(new FileInputStream(file2));

      // long startTime = System.currentTimeMillis();
      // 比较文件的长度是否一样
      if (inFile1.available() == inFile2.available()) {
        byte[] bytesFile1 = FileEx.readFully(inFile1);
        byte[] bytesFile2 = FileEx.readFully(inFile2);
        String md5File1 = Md5.getBytes(bytesFile1);
        String md5File2 = Md5.getBytes(bytesFile2);

        if (md5File1 == null || md5File2 == null) {
          isSame = false;
        } else if (!md5File1.equals(md5File2)) {
          isSame = true;
        }
      } else {
        isSame = false;
      }

      inFile1.close();
      inFile2.close();

      return isSame;

    } catch (IOException e) {
      LoggerUtils.LOGGER.error(ExceptionEx.e2s(e));
      return false;
    }
  }

  /**
   * 从txt文件中逐行读取字符串
   *
   * @param filePath 文件的路径
   * @return 读出的字符串集合ArrayList
   */
  public static ArrayList<String> readTxt(String filePath) {
    ArrayList<String> list = new ArrayList<String>();
    BufferedReader br = null;
    try {
      br =
          new BufferedReader(
              new InputStreamReader(new FileInputStream(filePath), StrEx.CHARSET_UTF8));
      String str = br.readLine();
      while (str != null) {
        list.add(str);
        str = br.readLine();
      }
    } catch (FileNotFoundException e) {
      LoggerUtils.LOGGER.error(ExceptionEx.e2s(e));
    } catch (IOException e) {
      LoggerUtils.LOGGER.error(ExceptionEx.e2s(e));
    } finally {
      if (br != null) {
        try {
          br.close();
        } catch (IOException e) {
          LoggerUtils.LOGGER.error(ExceptionEx.e2s(e));
        }
      }
    }
    return list;
  }
}
