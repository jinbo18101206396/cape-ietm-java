package org.jeecg.modules.ietm.icnmanage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.shiro.SecurityUtils;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.ietm.icnmanage.entity.IetmIcnManage;
import org.jeecg.modules.ietm.icnmanage.mapper.IetmIcnManageMapper;
import org.jeecg.modules.ietm.icnmanage.service.IIetmIcnManageService;
import org.jeecg.modules.ietm.ietmattachment.entity.IetmAttachment;
import org.jeecg.modules.ietm.ietmattachment.service.impl.IetmAttachmentServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import utils.DESUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

/**
 * @Description: 项目管理-项目实体管理
 * @Author: jeecg-boot
 * @Date: 2026-02-06
 * @Version: V1.0
 */
@Service
public class IetmIcnManageServiceImpl extends ServiceImpl<IetmIcnManageMapper, IetmIcnManage> implements IIetmIcnManageService {

    @Autowired
    private IetmAttachmentServiceImpl ietmAttachmentService;


    @Value("${accessFile.location}")
    private String location;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void fileAdd(MultipartFile[] files, String cmnodeId, Integer security, String uniqueId, String sns, String icnType, String variantCode, String issueNo, String originator, String originatorName, String rpc, String rpcName) throws IOException {
        IetmIcnManage ietmIcnManage = new IetmIcnManage();
        LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        String username = loginUser.getUsername();
        String pid = String.valueOf(IdWorker.getId());
        for (MultipartFile file : files) {
            IetmAttachment ietmAttachment = new IetmAttachment();
            // 文件名
            String filename = file.getOriginalFilename();
            ietmAttachment.setFileName(filename);
            //文件大小
            long size = file.getSize();
            ietmAttachment.setFileSize(new BigDecimal(size));
            String fileKey = "";
            if (filename.contains(".")) {
                fileKey = UUID.randomUUID() + filename.substring(filename.lastIndexOf("."));
            } else {
                fileKey = UUID.randomUUID() + "";
            }
            File file1 = new File(location);//加密文件夹
            String filePath = location + fileKey;//加密文件
            if (!file1.exists()) {
//                如果路径不存在，则创建所有必需的父目录
                if (!file1.mkdirs()) {
//                    如果创建失败，抛出异常
                    throw new IOException("无法创建目录：" + location);
                }
            }
            //文件加密
            InputStream inputs = file.getInputStream();
            try {
                DESUtils.encodeBase64File(inputs, filePath, null);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }

            ietmAttachment.setCreateBy(username);
            ietmAttachment.setFileType("实体文件");
            ietmAttachment.setCreateBy(username);
            ietmAttachment.setCreateTime(new Date());
            ietmAttachment.setFileKey(fileKey);
            ietmAttachment.setSecurity(security);
            ietmAttachment.setPid(pid);
            ietmAttachmentService.save(ietmAttachment);
        }
        ietmIcnManage.setId(pid);
        ietmIcnManage.setCmnodeId(cmnodeId);
        ietmIcnManage.setSecurity(security);
        ietmIcnManage.setUniqueId(uniqueId);
        ietmIcnManage.setSns(sns);
        ietmIcnManage.setIcnType(icnType);
        ietmIcnManage.setVariantCode(variantCode);
        ietmIcnManage.setIssueNo(issueNo);
        ietmIcnManage.setOriginator(originator);
        ietmIcnManage.setOriginatorName(originatorName);
        ietmIcnManage.setRpc(rpc);
        ietmIcnManage.setRpcName(rpcName);
        this.save(ietmIcnManage);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void relatedFilesAdd(MultipartFile[] files, String id)throws IOException {
        IetmIcnManage ietmIcnManage = this.baseMapper.selectById(id);
        LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        String username = loginUser.getUsername();
        for (MultipartFile file : files) {
            IetmAttachment ietmAttachment = new IetmAttachment();
            // 文件名
            String filename = file.getOriginalFilename();
            ietmAttachment.setFileName(filename);
            //文件大小
            long size = file.getSize();
            ietmAttachment.setFileSize(new BigDecimal(size));
            String fileKey = "";
            if (filename.contains(".")) {
                fileKey = UUID.randomUUID() + filename.substring(filename.lastIndexOf("."));
            } else {
                fileKey = UUID.randomUUID() + "";
            }
            File file1 = new File(location);//加密文件夹
            String filePath = location + fileKey;//加密文件
            if (!file1.exists()) {
//                如果路径不存在，则创建所有必需的父目录
                if (!file1.mkdirs()) {
//                    如果创建失败，抛出异常
                    throw new IOException("无法创建目录：" + location);
                }
            }
            //文件加密
            InputStream inputs = file.getInputStream();
            try {
                DESUtils.encodeBase64File(inputs, filePath, null);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
            IetmAttachment one = ietmAttachmentService.getOne(new QueryWrapper<IetmAttachment>().eq("pid", ietmIcnManage.getId()).eq("file_type", "相关文件"));

            if(one!=null){
                one.setUpdateBy(username);
                one.setUpdateTime(new Date());
                //文件名
                one.setFileName(filename);
                //文件大小
                one.setFileSize(new BigDecimal(size));
                one.setFileKey(fileKey);
                ietmAttachmentService.updateById(one);
            }else {
                ietmAttachment.setCreateBy(username);
                ietmAttachment.setFileType("相关文件");
                ietmAttachment.setCreateBy(username);
                ietmAttachment.setCreateTime(new Date());
                ietmAttachment.setFileKey(fileKey);
                ietmAttachment.setSecurity(ietmIcnManage.getSecurity());
                ietmAttachment.setPid(ietmIcnManage.getId());
            }
            ietmAttachmentService.saveOrUpdate(ietmAttachment);
        }
        //变更ICN码-去掉唯一标识码
        ietmIcnManage.setUniqueId("");
        this.updateById(ietmIcnManage);
    }

    @Override
    public void diffFilesAdd(MultipartFile[] files, String id, String uniqueId, String variantCode) throws IOException {
        IetmIcnManage ietmIcnManage = this.baseMapper.selectById(id);
        ietmIcnManage.setUniqueId(uniqueId);
        ietmIcnManage.setVariantCode(variantCode);
        String pid = String.valueOf(IdWorker.getId());
        ietmIcnManage.setId(pid);
        LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        String username = loginUser.getUsername();
        for (MultipartFile file : files) {
            IetmAttachment ietmAttachment = new IetmAttachment();
            // 文件名
            String filename = file.getOriginalFilename();
            ietmAttachment.setFileName(filename);
            //文件大小
            long size = file.getSize();
            ietmAttachment.setFileSize(new BigDecimal(size));
            String fileKey = "";
            if (filename.contains(".")) {
                fileKey = UUID.randomUUID() + filename.substring(filename.lastIndexOf("."));
            } else {
                fileKey = UUID.randomUUID() + "";
            }
            File file1 = new File(location);//加密文件夹
            String filePath = location + fileKey;//加密文件
            if (!file1.exists()) {
//                如果路径不存在，则创建所有必需的父目录
                if (!file1.mkdirs()) {
//                    如果创建失败，抛出异常
                    throw new IOException("无法创建目录：" + location);
                }
            }
            //文件加密
            InputStream inputs = file.getInputStream();
            try {
                DESUtils.encodeBase64File(inputs, filePath, null);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }

            ietmAttachment.setCreateBy(username);
            ietmAttachment.setFileType("实体文件");
            ietmAttachment.setCreateBy(username);
            ietmAttachment.setCreateTime(new Date());
            ietmAttachment.setFileKey(fileKey);
            ietmAttachment.setSecurity(ietmIcnManage.getSecurity());
            ietmAttachment.setPid(pid);
            ietmAttachmentService.save(ietmAttachment);
        }
        this.save(ietmIcnManage);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeAllByid(String id) {
        //删icn
        baseMapper.deleteById(id);
        //删附件数据
        ietmAttachmentService.remove(new QueryWrapper<IetmAttachment>().eq("pid", id));
        //删附件
        ietmAttachmentService.removeTempFileById(id);
    }

}
