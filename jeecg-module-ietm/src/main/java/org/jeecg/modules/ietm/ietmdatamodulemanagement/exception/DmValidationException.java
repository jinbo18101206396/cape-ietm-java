package org.jeecg.modules.ietm.ietmdatamodulemanagement.exception;

import lombok.Getter;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.vo.DmValidateItemVO;

import java.util.List;

/**
 * DM校验异常
 * 用于发布时Schema校验失败场景，封装详细错误列表
 *
 * @author Kiro AI
 * @date 2026-08-24
 */
@Getter
public class DmValidationException extends RuntimeException {

    /**
     * 校验错误列表（格式：[{lineno, info}]）
     */
    private final List<DmValidateItemVO> errors;

    /**
     * 构造函数
     *
     * @param message 概要错误信息
     * @param errors  详细错误列表
     */
    public DmValidationException(String message, List<DmValidateItemVO> errors) {
        super(message);
        this.errors = errors;
    }

    /**
     * 构造函数（仅概要信息）
     *
     * @param message 错误信息
     */
    public DmValidationException(String message) {
        super(message);
        this.errors = null;
    }
}
