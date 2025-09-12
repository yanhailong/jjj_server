package com.jjg.game.account.dto;

/**
 * @author 11
 * @date 2025/5/26 10:53
 */
public class GuestLoginDto extends LoginDto{
    private String guest;

    public String getGuest() {
        return guest;
    }

    public void setGuest(String guest) {
        this.guest = guest;
    }
}
