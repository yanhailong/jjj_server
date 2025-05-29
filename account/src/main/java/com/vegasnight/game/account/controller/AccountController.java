package com.vegasnight.game.account.controller;

import com.vegasnight.game.account.dao.AccountDao;
import com.vegasnight.game.account.dao.PlayerIdDao;
import com.vegasnight.game.account.logger.AccountLogger;
import com.vegasnight.game.core.dao.TokenDao;
import com.vegasnight.game.account.dto.GuestLoginDto;
import com.vegasnight.game.account.entity.Account;
import com.vegasnight.game.account.vo.LoginVo;
import com.vegasnight.game.account.vo.WebResult;
import com.vegasnight.game.core.constant.Code;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author 11
 * @date 2025/5/24 15:01
 */
@RestController
@RequestMapping(method = {RequestMethod.POST},value = "account")
public class AccountController extends AbstractController{

    @Autowired
    private AccountDao accountDao;

    @Autowired
    private PlayerIdDao playerIdDao;
    @Autowired
    private TokenDao tokenDao;
    @Autowired
    private AccountLogger accountLogger;


    /**
     * 游客登录
     * @param dto
     * @return
     */
    @RequestMapping("guestlogin")
    public WebResult<LoginVo> guestlogin(@RequestBody GuestLoginDto dto) {
        try{
            accountLogger.test(dto.guest);
            if(StringUtils.isEmpty(dto.guest)){
                log.debug("参数为空，游客登录失败");
                return fail(Code.PARAM_ERROR);
            }

            Account account = accountDao.queryAccountByGuest(dto.guest);

            if(account == null){
                //注册新账号
                long playerId = playerIdDao.getNewId();
                account = new Account();
                account.setPlayerId(playerId);
                account.setGuest(dto.guest);
                account = accountDao.insert(account);

                log.debug("创建新的游客账号 guest = {},playerId = {}",dto.guest,playerId);
            }

            //生成token
            String token = genernateToken();
            //保存token，方便weboskcet连接时进行校验
            tokenDao.save(token, account.getPlayerId());

            LoginVo vo = new LoginVo();
            vo.setToken(token);
            vo.setGameserver("ws://192.168.3.31:8090");
            log.info("游客登录成功 guest = {},playerId = {}",dto.guest,account.getPlayerId());
            return success(vo);
        }catch (Exception e){
            log.error("",e);
            return exception();
        }
    }
}
