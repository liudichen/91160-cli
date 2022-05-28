package com.github.pengpan.cmd;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.setting.dialect.Props;
import com.github.pengpan.entity.Config;
import com.github.pengpan.service.CoreService;
import com.github.pengpan.util.Assert;
import io.airlift.airline.Command;
import io.airlift.airline.Option;
import lombok.extern.slf4j.Slf4j;

/**
 * @author pengpan
 */
@Slf4j
@Command(name = "register", description = "Register on 91160.com")
public class Register implements Runnable {

    @Option(
            name = {"-c", "--config"},
            title = "configuration file",
            required = true,
            description = "Path to json configuration file.")
    private String configFile;

    @Override
    public void run() {
        Assert.isTrue(FileUtil.exist(configFile), "配置文件不存在，请检查文件路径");
        Assert.notBlank(FileUtil.readUtf8String(configFile), "配置文件不能为空");
        Assert.isTrue(configFile.endsWith(Props.EXT_NAME), "配置文件不正确");

        Props props = new Props(FileUtil.file(configFile), CharsetUtil.CHARSET_UTF_8);
        Config config = new Config();
        props.fillBean(config, null);

        CoreService coreService = SpringUtil.getBean(CoreService.class);
        checkConfig(config, coreService);

        try {
            coreService.brushTicketTask(config);
            System.exit(0);
        } catch (Exception e) {
            log.error("", e);
            System.exit(-1);
        }
    }

    private void checkConfig(Config config, CoreService coreService) {
        // Required
        Assert.notBlank(config.getUserName(), "[userName]不能为空，请检查配置文件");
        Assert.notBlank(config.getPassword(), "[password]不能为空，请检查配置文件");
        Assert.isTrue(coreService.login(config.getUserName(), config.getPassword()), "登录失败，请检查账号和密码");
        Assert.notBlank(config.getMemberId(), "[memberId]不能为空，请检查配置文件");
        Assert.notBlank(config.getCityId(), "[cityId]不能为空，请检查配置文件");
        Assert.notBlank(config.getUnitId(), "[unitId]不能为空，请检查配置文件");
        Assert.notBlank(config.getDeptId(), "[deptId]不能为空，请检查配置文件");
        Assert.notBlank(config.getDoctorId(), "[doctorId]不能为空，请检查配置文件");
        Assert.isTrue(CollUtil.isNotEmpty(config.getWeeks()), "[weeks]不能为空，请检查配置文件");
        Assert.isTrue(CollUtil.isNotEmpty(config.getDays()), "[days]不能为空，请检查配置文件");
        Assert.isTrue(config.getSleepTime() >= 0, "[sleepTime]格式不正确，请检查配置文件");

        // Not required
        if (StrUtil.isNotBlank(config.getAppointTime())) {
            boolean r = true;
            try {
                DateUtil.parse(config.getAppointTime(), DatePattern.NORM_DATETIME_PATTERN);
            } catch (Exception ignored) {
                r = false;
            }
            Assert.isTrue(r, "[appointTime]格式不正确，请检查配置文件");
        }
        if (StrUtil.isNotBlank(config.getGetProxyURL())) {
            Assert.isTrue(Validator.isUrl(config.getGetProxyURL()), "[getProxyURL]格式不正确，请检查配置文件");
        }
    }
}
