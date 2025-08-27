package com.jjg.game.core.data;

import org.springframework.data.mongodb.core.mapping.Document;

/**
 * 押注类好友房间
 *
 * @author 2CL
 */
@Document(collection = "BetFriendRoom")
public class BetFriendRoom extends FriendRoom {
}
