package com.jjg.game.core.net.message;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/** 线程安全的消息池 */
public class SMessageFactory {

  private final Queue<SMessage> sMessageQueue = new ConcurrentLinkedQueue<>();

  public SMessageFactory(int size) {
    for (int i = 0; i < size; ++i) {
      SMessage sMessage = new SMessage(this);
      sMessageQueue.offer(sMessage);
    }
  }

  public SMessage fetchSMessage(int msgId, byte[] msgData) {
    SMessage sMsg = null;

    if (sMessageQueue.peek() != null) {
      sMsg = sMessageQueue.poll();
    }

    if (sMsg == null) {
      sMsg = new SMessage(msgId, msgData);
    } else {
      sMsg.setId(msgId);
      sMsg.setData(msgData);
    }
    return sMsg;
  }

  public void recycleSMessage(SMessage sMessage) {
    sMessage.clear();
    sMessageQueue.offer(sMessage);
  }
}
